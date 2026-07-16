# message-relay

Kafka-driven notification delivery engine for SMS, Email, WhatsApp, and payment notification streams.

## What this service does

`message-relay` consumes `NotificationEvent` payloads from Kafka, validates input, routes by channel to sender implementations, persists delivery lifecycle to MongoDB, indexes searchable delivery projections in Elasticsearch, and handles failures via retry, circuit breaker, and DLQ.

## Core capabilities

- Channel-based Kafka consumers (`email`, `sms`, `whatsapp`, `payment`)
- Validation and PII masking before operational exposure
- Sender strategy pattern (`MessageSender` + factory)
- Resilience via Resilience4j retry and circuit breaker
- Durable delivery audit trail with status history in MongoDB
- Search and filtering over Elasticsearch projection
- DLQ publishing and Sentry alerting for exhausted failures
- Health and Prometheus metrics for observability

## Architecture at a glance

Flow:
1. Producer publishes event to topic
2. Consumer receives event (manual ack)
3. `DeliveryService` validates and creates `DeliveryRecord`
4. Sender is resolved by channel and delivery is attempted
5. On success: MongoDB update + Elasticsearch index
6. On exhausted failures: `DEAD_LETTERED` + DLQ publish + Sentry alert

## Key endpoints

- `GET /api/messages/search`
- `GET /api/messages/{eventId}`
- `GET /api/messages/stats/dlq`
- `GET /actuator/health`
- `GET /actuator/prometheus`

## Documentation

All detailed documentation is markdown-only in `docs/`:

- `docs/BRD.md` - Business Requirements Document
- `docs/PRD.md` - Product Requirements Document
- `docs/HLD.md` - High Level Design
- `docs/LLD.md` - Low Level Design
- `docs/API-SPECS.md` - API contracts, request/response models, and error semantics

## Local run (Windows PowerShell)

```powershell
cd C:\Users\prash\Desktop\GITHUB_God_Incarnate\message-relay

docker-compose up -d

.\mvnw.cmd spring-boot:run
```

## Build and test

```powershell
cd C:\Users\prash\Desktop\GITHUB_God_Incarnate\message-relay

.\mvnw.cmd clean test

.\mvnw.cmd clean package
```

## Tech stack

- Java 17
- Spring Boot
- Spring Kafka
- Spring Data MongoDB
- Spring Data Elasticsearch
- Resilience4j
- Micrometer + Prometheus
- Sentry

## Important notes

- Delivery guarantee is at-least-once; consumers use manual acknowledgement.
- `recipient` exposed operationally is masked; raw recipient handling is restricted.
- Elasticsearch is a secondary index; MongoDB is the source of truth.

