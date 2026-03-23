package com.prashant.message_relay.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "delivery_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRecord {

    @Id
    private String id;

    @Indexed
    private String eventId;

    @Indexed
    private String clientId;

    @Indexed
    private String recipient;       // masked for PII (e.g. +91****7890)

    private String recipientRaw;    // stored encrypted — not indexed

    private NotificationEvent.Channel channel;

    private String templateId;

    private Map<String, String> templateParams;

    @Indexed
    private DeliveryStatus status;

    @Builder.Default
    private List<StatusTransition> statusHistory = new ArrayList<>();

    @Builder.Default
    private int attemptCount = 0;

    private String failureReason;

    private String vendorMessageId;

    @Indexed
    private LocalDateTime createdAt;

    private LocalDateTime deliveredAt;

    private String correlationId;

    public enum DeliveryStatus {
        PENDING, VALIDATING, SENDING, SENT, FAILED, RETRYING, DEAD_LETTERED
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusTransition {
        private DeliveryStatus from;
        private DeliveryStatus to;
        private String reason;
        private LocalDateTime at;
    }

    public void addTransition(DeliveryStatus from, DeliveryStatus to, String reason) {
        this.statusHistory.add(new StatusTransition(from, to, reason, LocalDateTime.now()));
        this.status = to;
    }
}
