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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
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

    public DeliveryService(PayloadValidator validator,
                           MessageSenderFactory senderFactory,
                           DeliveryRecordRepository recordRepository,
                           DeliverySearchRepository searchRepository,
                           DlqHandler dlqHandler,
                           MeterRegistry meterRegistry) {
        this.validator = validator;
        this.senderFactory = senderFactory;
        this.recordRepository = recordRepository;
        this.searchRepository = searchRepository;
        this.dlqHandler = dlqHandler;
        this.successCounter = meterRegistry.counter("delivery.success");
        this.failureCounter = meterRegistry.counter("delivery.failure");
    }

    public void process(NotificationEvent event) {
        // 1. Validate payload
        PayloadValidator.ValidationResult validation = validator.validate(event);
        if (!validation.valid()) {
            log.warn("Invalid payload eventId={} errors={}", event.getEventId(), validation.errors());
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

        // 3. Send with retry + circuit breaker
        attemptDelivery(event, record);
    }

    @CircuitBreaker(name = "messageSender", fallbackMethod = "deliveryFallback")
    @Retry(name = "messageSender", fallbackMethod = "deliveryFallback")
    public void attemptDelivery(NotificationEvent event, DeliveryRecord record) {
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

        // Index in Elasticsearch
        indexToElasticsearch(record);

        successCounter.increment();
        log.info("Delivered eventId={} channel={} vendorId={}", event.getEventId(), event.getChannel(), vendorId);
    }

    public void deliveryFallback(NotificationEvent event, DeliveryRecord record, Throwable ex) {
        log.error("Delivery failed after retries eventId={} error={}", event.getEventId(), ex.getMessage());

        record.setFailureReason(ex.getMessage());
        record.addTransition(record.getStatus(), DeliveryRecord.DeliveryStatus.DEAD_LETTERED,
                "Exhausted retries: " + ex.getMessage());
        recordRepository.save(record);

        indexToElasticsearch(record);

        failureCounter.increment();
        dlqHandler.sendToDlq(event, ex.getMessage());
    }

    private void indexToElasticsearch(DeliveryRecord record) {
        try {
             DeliveryDocument doc = DeliveryDocument.builder()
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
            searchRepository.save(doc);
        } catch (Exception e) {
            log.warn("Failed to index eventId={} to Elasticsearch: {}", record.getEventId(), e.getMessage());
        }
    }
}
