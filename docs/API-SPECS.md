# message-relay - API Specifications

Version: 1.0
Base path: `/api/messages`
Content type: `application/json`

## 1) Delivery Search

`GET /api/messages/search`

Searches delivery records from Elasticsearch with pagination.

### Query Parameters

| Name | Type | Required | Default | Notes |
|---|---|---|---|---|
| `recipient` | string | No | - | Masked value expected |
| `status` | enum | No | - | `PENDING`, `SENDING`, `SENT`, `FAILED`, `RETRYING`, `DEAD_LETTERED` |
| `channel` | enum | No | - | `SMS`, `EMAIL`, `WHATSAPP` |
| `clientId` | string | No | - | Client identifier |
| `page` | integer | No | `0` | Zero-based index |
| `size` | integer | No | `20` | Page size |

### Resolution Logic

1. `clientId + status` -> `findByClientIdAndStatus`
2. `channel + status` -> `findByChannelAndStatus`
3. `recipient` -> `findByRecipient`
4. `clientId` -> `findByClientId`
5. `status` -> `findByStatus`
6. none -> `findAll`

Sorted by `createdAt DESC`.

### Success Response (200)

```json
{
  "content": [
    {
      "id": "68817f7c4e2f95a7b53f1001",
      "eventId": "evt-123",
      "clientId": "client-a",
      "recipient": "+91****7890",
      "channel": "SMS",
      "templateId": "otp-template",
      "status": "SENT",
      "attemptCount": 1,
      "failureReason": null,
      "correlationId": "corr-otp-1",
      "createdAt": "2026-07-16T18:30:00",
      "deliveredAt": "2026-07-16T18:30:02"
    }
  ],
  "pageable": {},
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

## 2) Delivery Detail by Event ID

`GET /api/messages/{eventId}`

Fetches full delivery record from MongoDB including status history.

### Path Parameters

| Name | Type | Required | Notes |
|---|---|---|---|
| `eventId` | string | Yes | Event identifier |

### Success Response (200)

```json
{
  "id": "68817f7c4e2f95a7b53f1001",
  "eventId": "evt-123",
  "clientId": "client-a",
  "recipient": "+91****7890",
  "recipientRaw": "+911234567890",
  "channel": "SMS",
  "templateId": "otp-template",
  "templateParams": { "otp": "123456" },
  "status": "SENT",
  "statusHistory": [
    { "from": null, "to": "PENDING", "reason": "Created", "at": "2026-07-16T18:30:00" },
    { "from": "PENDING", "to": "SENDING", "reason": "Attempt 1", "at": "2026-07-16T18:30:01" },
    { "from": "SENDING", "to": "SENT", "reason": "Vendor ACK: SMS-abc123", "at": "2026-07-16T18:30:02" }
  ],
  "attemptCount": 1,
  "failureReason": null,
  "vendorMessageId": "SMS-abc123",
  "createdAt": "2026-07-16T18:30:00",
  "deliveredAt": "2026-07-16T18:30:02",
  "correlationId": "corr-otp-1"
}
```

### Not Found (404)

Empty body.

## 3) Delivery Stats / DLQ Summary

`GET /api/messages/stats/dlq`

Returns delivery summary and success rate from MongoDB counts.

### Success Response (200)

```json
{
  "total": 15420,
  "sent": 15398,
  "deadLettered": 8,
  "pending": 14,
  "successRatePct": "99.95"
}
```

## 4) Actuator Endpoints

These are exposed by Spring Actuator configuration.

- `GET /actuator/health`
- `GET /actuator/prometheus`

Notes:
- Health may return `UP`, `DEGRADED`, or `DOWN` based on dependency checks.
- Prometheus endpoint exports delivery counters and circuit state gauge.

## 5) Enums and Status Semantics

### Channel
- `SMS`
- `EMAIL`
- `WHATSAPP`

### Delivery Status
- `PENDING`
- `VALIDATING`
- `SENDING`
- `SENT`
- `FAILED`
- `RETRYING`
- `DEAD_LETTERED`

## 6) Error Handling Contract

Current controller behavior relies on Spring defaults:
- Invalid enum request parameter -> `400 Bad Request`
- Unknown route -> `404 Not Found`
- Missing record by `eventId` -> `404 Not Found`

Operational errors are logged and handled in service layer, with failed deliveries routed to DLQ where applicable.

## 7) Security and Data Exposure Notes

- `recipient` is intended to be masked in search and operational contexts.
- `recipientRaw` appears in persistence model and detail response as currently implemented; consider response DTO redaction if strict API masking is required.
- API auth is not described in current implementation docs; gateway-level controls are recommended for production.

