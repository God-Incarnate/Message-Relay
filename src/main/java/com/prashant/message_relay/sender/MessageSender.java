package com.prashant.message_relay.sender;


import com.prashant.message_relay.model.DeliveryRecord;
import com.prashant.message_relay.model.NotificationEvent;

public interface MessageSender {
    /**
     * Send the notification. Returns vendor message ID on success.
     * Throws RuntimeException on failure (triggers retry).
     */
    String send(NotificationEvent event, DeliveryRecord record);

    NotificationEvent.Channel supportedChannel();
}
