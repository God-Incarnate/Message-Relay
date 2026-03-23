package com.prashant.message_relay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventId;
    private String clientId;
    private String recipient;
    private Channel channel;
    private String templateId;
    private Map<String, String> templateParams;
    private Priority priority;
    private LocalDateTime createdAt;
    private String correlationId;

    public enum Channel { SMS, EMAIL, WHATSAPP }
    public enum Priority { HIGH, MEDIUM, LOW }
}
