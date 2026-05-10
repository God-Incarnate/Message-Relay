package com.prashant.message_relay.retry;

import com.prashant.message_relay.model.NotificationEvent;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.dlq}")
    private String dlqTopic;

    public void sendToDlq(NotificationEvent event, String reason) {
        log.error("Sending to DLQ eventId={} channel={} reason={}",
                event.getEventId(), event.getChannel(), reason);

        // Publish to DLQ topic
        kafkaTemplate.send(dlqTopic, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to DLQ eventId={}", event.getEventId(), ex);
                    } else {
                        log.info("DLQ published eventId={} partition={} offset={}",
                                event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });

        // Alert via Sentry
        alertSentry(event, reason);
    }

    private void alertSentry(NotificationEvent event, String reason) {
        try {
            Sentry.withScope(scope -> {
                scope.setLevel(SentryLevel.ERROR);
                scope.setTag("eventId", event.getEventId());
                scope.setTag("channel", event.getChannel() != null ? event.getChannel().name() : "UNKNOWN");
                scope.setTag("clientId", event.getClientId());
                scope.setExtra("recipient", event.getRecipient());
                scope.setExtra("templateId", event.getTemplateId());
                scope.setExtra("correlationId", event.getCorrelationId());
                scope.setExtra("reason", reason);
                Sentry.captureMessage(
                    String.format("DLQ: Message delivery failed [%s] eventId=%s reason=%s",
                        event.getChannel(), event.getEventId(), reason)
                );
            });
        } catch (Exception e) {
            log.warn("Sentry alert failed for eventId={}: {}", event.getEventId(), e.getMessage());
        }
    }
}
