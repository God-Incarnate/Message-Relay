package com.prashant.message_relay.sender;

import com.prashant.message_relay.model.DeliveryRecord;
import com.prashant.message_relay.model.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class SmsSender implements MessageSender {

    @Override
    public String send(NotificationEvent event, DeliveryRecord record) {
        // TODO: Replace with real Twilio / Gupshup SDK call
        log.info("SMS sending to={} templateId={} correlationId={}",
                record.getRecipient(), event.getTemplateId(), event.getCorrelationId());

        // Simulate vendor call
        simulateVendorCall();

        String vendorId = "SMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("SMS delivered vendorId={}", vendorId);
        return vendorId;
    }

    @Override
    public NotificationEvent.Channel supportedChannel() {
        return NotificationEvent.Channel.SMS;
    }

    private void simulateVendorCall() {
        // Simulate ~50ms network call
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
