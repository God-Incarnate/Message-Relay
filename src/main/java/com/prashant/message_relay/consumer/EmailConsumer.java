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
public class EmailConsumer {

    private final DeliveryService deliveryService;

    @KafkaListener(
        topics = "${kafka.topics.email}",
        groupId = "delivery-engine-group",
        concurrency = "3"
    )

}
