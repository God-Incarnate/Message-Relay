package com.prashant.message_relay.consumer;

import com.prashant.message_relay.model.NotificationEvent;
import com.prashant.message_relay.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsConsumer {

    private final DeliveryService deliveryService;

    @KafkaListener(
        topics = "${kafka.topics.sms}",
        groupId = "delivery-engine-group",
        concurrency = "3"
    )
    public void consume(NotificationEvent event,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        Acknowledgment ack) {
        log.info("SMS consumed eventId={} partition={} offset={}", event.getEventId(), partition, offset);
        try {
            deliveryService.process(event);
        } catch (Exception e) {
            // process() has its own safety-net catch and should never throw, but guard here
            // to guarantee we always commit the offset and don't stall the partition.
            log.error("SMS consumer unexpected error eventId={} partition={} offset={}", event.getEventId(), partition, offset, e);
        } finally {
            ack.acknowledge();
        }
    }
}
