package com.prashant.message_relay.service;

import com.prashant.message_relay.model.DeliveryRecord;
import com.prashant.message_relay.model.NotificationEvent;
import com.prashant.message_relay.repository.DeliveryRecordRepository;
import com.prashant.message_relay.repository.DeliverySearchRepository;
import com.prashant.message_relay.retry.DlqHandler;
import com.prashant.message_relay.sender.MessageSender;
import com.prashant.message_relay.sender.MessageSenderFactory;
import com.prashant.message_relay.validator.PayloadValidator;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private PayloadValidator validator;

    @Mock
    private MessageSenderFactory senderFactory;

    @Mock
    private DeliveryRecordRepository recordRepository;

    @Mock
    private DeliverySearchRepository searchRepository;

    @Mock
    private DlqHandler dlqHandler;

    @Mock
    private MessageSender messageSender;

    @Test
    void shouldIncrementValidationFailedMetricAndSendToDlqForInvalidPayload() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DeliveryService service = new DeliveryService(
                validator,
                senderFactory,
                recordRepository,
                searchRepository,
                dlqHandler,
                meterRegistry,
                CircuitBreakerRegistry.ofDefaults()
        );

        NotificationEvent event = NotificationEvent.builder()
                .eventId("evt-invalid")
                .clientId("client-1")
                .recipient("bad-recipient")
                .channel(NotificationEvent.Channel.SMS)
                .priority(NotificationEvent.Priority.HIGH)
                .build();

        when(validator.validate(event)).thenReturn(PayloadValidator.ValidationResult.fail(List.of("recipient invalid")));

        service.process(event);

        assertEquals(1.0, meterRegistry.counter("delivery.validation.failed").count());
        verify(dlqHandler).sendToDlq(eq(event), eq("VALIDATION_FAILED: [recipient invalid]"));
        verify(recordRepository, never()).save(any(DeliveryRecord.class));
        verify(searchRepository, never()).save(any());
    }

    @Test
    void shouldIncrementRetryAttemptMetricOnSecondAttempt() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DeliveryService service = new DeliveryService(
                validator,
                senderFactory,
                recordRepository,
                searchRepository,
                dlqHandler,
                meterRegistry,
                CircuitBreakerRegistry.ofDefaults()
        );

        NotificationEvent event = NotificationEvent.builder()
                .eventId("evt-retry")
                .clientId("client-1")
                .recipient("+911234567890")
                .channel(NotificationEvent.Channel.SMS)
                .priority(NotificationEvent.Priority.HIGH)
                .templateId("otp")
                .templateParams(Map.of("code", "1234"))
                .correlationId("corr-1")
                .build();

        DeliveryRecord record = DeliveryRecord.builder()
                .id("mongo-1")
                .eventId("evt-retry")
                .clientId("client-1")
                .recipient("+91****7890")
                .recipientRaw("+911234567890")
                .channel(NotificationEvent.Channel.SMS)
                .status(DeliveryRecord.DeliveryStatus.SENDING)
                .attemptCount(1)
                .createdAt(LocalDateTime.now())
                .build();

        when(senderFactory.getSender(NotificationEvent.Channel.SMS)).thenReturn(messageSender);
        when(messageSender.send(event, record)).thenReturn("vendor-1");
        when(recordRepository.save(any(DeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.attemptDelivery(event, record);

        assertEquals(1.0, meterRegistry.counter("delivery.retry.attempt").count());
        verify(searchRepository).save(any());
    }

    @Test
    void shouldNotFailDeliveryWhenElasticsearchIndexingFails() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DeliveryService service = new DeliveryService(
                validator,
                senderFactory,
                recordRepository,
                searchRepository,
                dlqHandler,
                meterRegistry,
                CircuitBreakerRegistry.ofDefaults()
        );

        NotificationEvent event = NotificationEvent.builder()
                .eventId("evt-es-down")
                .clientId("client-1")
                .recipient("+911234567890")
                .channel(NotificationEvent.Channel.SMS)
                .priority(NotificationEvent.Priority.HIGH)
                .build();

        DeliveryRecord record = DeliveryRecord.builder()
                .id("mongo-2")
                .eventId("evt-es-down")
                .clientId("client-1")
                .recipient("+91****7890")
                .recipientRaw("+911234567890")
                .channel(NotificationEvent.Channel.SMS)
                .status(DeliveryRecord.DeliveryStatus.PENDING)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(senderFactory.getSender(NotificationEvent.Channel.SMS)).thenReturn(messageSender);
        when(messageSender.send(event, record)).thenReturn("vendor-es-1");
        when(recordRepository.save(any(DeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(searchRepository.save(any())).thenThrow(new RuntimeException("elasticsearch unavailable"));

        assertDoesNotThrow(() -> service.attemptDelivery(event, record));

        assertEquals(1.0, meterRegistry.counter("delivery.success").count());
        verify(dlqHandler, never()).sendToDlq(any(), any());
    }
}


