package com.prashant.message_relay.sender;

import com.prashant.message_relay.model.DeliveryRecord;
import com.prashant.message_relay.model.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class WhatsAppSender implements MessageSender {

    @Override
    public String send(NotificationEvent event, DeliveryRecord record) {
        // TODO: Replace with WhatsApp Business API / Gupshup SDK
        log.info("WhatsApp sending to={} templateId={}", record.getRecipient(), event.getTemplateId());
        try { Thread.sleep(70); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        String vendorId = "WA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("WhatsApp delivered vendorId={}", vendorId);
        return vendorId;
    }

    @Override
    public NotificationEvent.Channel supportedChannel() {
        return NotificationEvent.Channel.WHATSAPP;
    }
}
