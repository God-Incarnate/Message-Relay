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
public class PaymentConsumer {

    private final DeliveryService deliveryService;

    @KafkaListener(
        topics = "${kafka.topics.payment}",
        groupId = "delivery-engine-group",
        concurrency = "3"
    )
    public void consume(NotificationEvent event,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        Acknowledgment ack) {
        log.debug("Payment consumer received eventId={} partition={} offset={}",
                event.getEventId(), partition, offset);
        try {
            deliveryService.process(event);
        } catch (Exception e) {
            log.error("Payment consumer unexpected error eventId={} partition={} offset={}", event.getEventId(), partition, offset, e);
        } finally {
            ack.acknowledge();
        }
    }
}
