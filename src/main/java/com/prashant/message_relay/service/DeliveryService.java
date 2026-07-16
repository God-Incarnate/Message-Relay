package com.prashant.message_relay.service;


import com.prashant.message_relay.model.DeliveryDocument;
import com.prashant.message_relay.model.DeliveryRecord;
import com.prashant.message_relay.model.NotificationEvent;
import com.prashant.message_relay.repository.DeliveryRecordRepository;
import com.prashant.message_relay.repository.DeliverySearchRepository;
import com.prashant.message_relay.retry.DlqHandler;
import com.prashant.message_relay.sender.MessageSender;
import com.prashant.message_relay.sender.MessageSenderFactory;
import com.prashant.message_relay.validator.PayloadValidator;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class DeliveryService {

    private final PayloadValidator validator;
    private final MessageSenderFactory senderFactory;
    private final DeliveryRecordRepository recordRepository;
    private final DeliverySearchRepository searchRepository;
    private final DlqHandler dlqHandler;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter validationFailedCounter;
    private final Counter retryAttemptCounter;

    // Self-injection via @Lazy so that @CircuitBreaker / @Retry AOP proxies are
    // honoured on attemptDelivery() — direct `this.*` calls bypass Spring AOP.
    @Autowired
    @Lazy
    private DeliveryService self;

    public DeliveryService(PayloadValidator validator,
                           MessageSenderFactory senderFactory,
                           DeliveryRecordRepository recordRepository,
                           DeliverySearchRepository searchRepository,
                           DlqHandler dlqHandler,
                           MeterRegistry meterRegistry,
                           CircuitBreakerRegistry circuitBreakerRegistry) {
        this.validator = validator;
        this.senderFactory = senderFactory;
        this.recordRepository = recordRepository;
        this.searchRepository = searchRepository;
        this.dlqHandler = dlqHandler;
        this.successCounter = meterRegistry.counter("delivery.success");
        this.failureCounter = meterRegistry.counter("delivery.failure");
        this.validationFailedCounter = meterRegistry.counter("delivery.validation.failed");
        this.retryAttemptCounter = meterRegistry.counter("delivery.retry.attempt");
        Gauge.builder("delivery.circuit.open", () -> isCircuitOpen(circuitBreakerRegistry))
                .description("1 when message sender circuit breaker is open")
                .register(meterRegistry);
    }

    public void process(NotificationEvent event) {
        try {
            // 1. Validate payload
            PayloadValidator.ValidationResult validation = validator.validate(event);
            if (!validation.valid()) {
                log.warn("Invalid payload eventId={} errors={}", event.getEventId(), validation.errors());
                validationFailedCounter.increment();
                // Don't retry invalid payloads — save to DLQ immediately
                dlqHandler.sendToDlq(event, "VALIDATION_FAILED: " + validation.errors());
                return;
            }

            // 2. Create delivery record
            String maskedRecipient = validator.maskPii(event.getRecipient(), event.getChannel());
            DeliveryRecord record = DeliveryRecord.builder()
                    .eventId(event.getEventId())
                    .clientId(event.getClientId())
                    .recipient(maskedRecipient)
                    .recipientRaw(event.getRecipient())
                    .channel(event.getChannel())
                    .templateId(event.getTemplateId())
                    .templateParams(event.getTemplateParams())
                    .status(DeliveryRecord.DeliveryStatus.PENDING)
                    .correlationId(event.getCorrelationId())
                    .createdAt(LocalDateTime.now())
                    .build();

            record.addTransition(null, DeliveryRecord.DeliveryStatus.PENDING, "Created");
            record = recordRepository.save(record);

            // 3. Send via proxy so @CircuitBreaker / @Retry AOP interceptors fire
            self.attemptDelivery(event, record);

        } catch (Exception e) {
            // Safety net: fallback may itself throw (e.g. MongoDB down before record is created).
            // Ensure we always send to DLQ and return normally so the Kafka consumer can ack.
            log.error("Unexpected error in process() eventId={} — sending to DLQ", event.getEventId(), e);
            failureCounter.increment();
            try {
                dlqHandler.sendToDlq(event, "PROCESS_ERROR: " + e.getMessage());
            } catch (Exception dlqEx) {
                log.error("DLQ send also failed eventId={}", event.getEventId(), dlqEx);
            }
        }
    }

    @CircuitBreaker(name = "messageSender", fallbackMethod = "deliveryFallback")
    @Retry(name = "messageSender", fallbackMethod = "deliveryFallback")
    public void attemptDelivery(NotificationEvent event, DeliveryRecord record) {
        if (record.getAttemptCount() > 0) {
            retryAttemptCounter.increment();
        }

        record.addTransition(record.getStatus(), DeliveryRecord.DeliveryStatus.SENDING, "Attempt " + (record.getAttemptCount() + 1));
        record.setAttemptCount(record.getAttemptCount() + 1);
        recordRepository.save(record);

        MessageSender sender = senderFactory.getSender(event.getChannel());
        String vendorId = sender.send(event, record);

        // Success
        record.setVendorMessageId(vendorId);
        record.setDeliveredAt(LocalDateTime.now());
        record.addTransition(DeliveryRecord.DeliveryStatus.SENDING, DeliveryRecord.DeliveryStatus.SENT, "Vendor ACK: " + vendorId);
        recordRepository.save(record);
        indexToSearch(record);

        successCounter.increment();
        log.info("Delivered eventId={} channel={} vendorId={}", event.getEventId(), event.getChannel(), vendorId);
    }

    public void deliveryFallback(NotificationEvent event, DeliveryRecord record, Throwable ex) {
        log.error("Delivery failed after retries eventId={} error={}", event.getEventId(), ex.getMessage());

        record.setFailureReason(ex.getMessage());
        record.addTransition(record.getStatus(), DeliveryRecord.DeliveryStatus.DEAD_LETTERED,
                "Exhausted retries: " + ex.getMessage());
        recordRepository.save(record);
        indexToSearch(record);

        failureCounter.increment();
        dlqHandler.sendToDlq(event, ex.getMessage());
    }

    private void indexToSearch(DeliveryRecord record) {
        try {
            searchRepository.save(toDocument(record));
        } catch (Exception e) {
            log.warn("ES index failed for eventId={}: {}", record.getEventId(), e.getMessage());
        }
    }

    private DeliveryDocument toDocument(DeliveryRecord record) {
        return DeliveryDocument.builder()
                .id(record.getId())
                .eventId(record.getEventId())
                .clientId(record.getClientId())
                .recipient(record.getRecipient())
                .channel(record.getChannel() != null ? record.getChannel().name() : null)
                .templateId(record.getTemplateId())
                .status(record.getStatus() != null ? record.getStatus().name() : null)
                .attemptCount(record.getAttemptCount())
                .failureReason(record.getFailureReason())
                .correlationId(record.getCorrelationId())
                .createdAt(record.getCreatedAt())
                .deliveredAt(record.getDeliveredAt())
                .build();
    }

    private double isCircuitOpen(CircuitBreakerRegistry circuitBreakerRegistry) {
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("messageSender");
        return circuitBreaker.getState() == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN ? 1.0 : 0.0;
    }
}
