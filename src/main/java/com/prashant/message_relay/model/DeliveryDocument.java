package com.prashant.message_relay.model;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

/**
 * DeliveryDocument kept as a plain POJO. Elasticsearch annotations removed as the
 * project no longer uses Elasticsearch. This class remains to preserve shape used
 * by other parts of the system (if any), but can be deleted entirely if not needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDocument {

    @Id
    private String id;

    private String eventId;
    private String clientId;
    private String recipient;   // masked
    private String channel;
    private String templateId;
    private String status;
    private int attemptCount;
    private String failureReason;
    private String correlationId;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
}
