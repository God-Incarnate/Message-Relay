package com.prashant.message_relay.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "delivery_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String eventId;

    @Field(type = FieldType.Keyword)
    private String clientId;

    @Field(type = FieldType.Keyword)
    private String recipient;   // masked

    @Field(type = FieldType.Keyword)
    private String channel;

    @Field(type = FieldType.Keyword)
    private String templateId;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Integer)
    private int attemptCount;

    @Field(type = FieldType.Text)
    private String failureReason;

    @Field(type = FieldType.Keyword)
    private String correlationId;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime deliveredAt;
}
