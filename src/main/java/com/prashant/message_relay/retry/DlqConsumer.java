package com.prashant.message_relay.retry;

import com.prashant.message_relay.model.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DlqConsumer {

    /**
     * Reads from the DLQ topic for visibility and manual reprocessing.
     * In production: expose a REST endpoint to replay selected DLQ events.
     */
    @KafkaListener(
        topics = "${kafka.topics.dlq}",
        groupId = "dlq-monitor-group",
        concurrency = "1"
    )
    public void monitorDlq(NotificationEvent event,
                           @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                           @Header(KafkaHeaders.OFFSET) long offset,
                           Acknowledgment ack) {
        log.error("[DLQ MONITOR] eventId={} channel={} clientId={} templateId={} partition={} offset={}",
                event.getEventId(),
                event.getChannel(),
                event.getClientId(),
                event.getTemplateId(),
                partition,
                offset);
        // Just acknowledge — do not reprocess automatically
        // Manual replay endpoint: POST /api/dlq/{eventId}/replay
        ack.acknowledge();
    }
}
