package com.prashant.message_relay.sender;

import com.prashant.message_relay.model.NotificationEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MessageSenderFactory {

    private final Map<NotificationEvent.Channel, MessageSender> senderMap;

    public MessageSenderFactory(List<MessageSender> senders) {
        this.senderMap = senders.stream()
                .collect(Collectors.toMap(MessageSender::supportedChannel, Function.identity()));
    }

    public MessageSender getSender(NotificationEvent.Channel channel) {
        MessageSender sender = senderMap.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("No sender registered for channel: " + channel);
        }
        return sender;
    }
}
