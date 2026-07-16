# Product Requirements Document (PRD)

This folder contains the **Product Requirements Document (PRD)** for the message-relay project.

## Contents

- `message-relay-PRD.md` — Source markdown file containing the complete PRD
- `message-relay-PRD.pdf` — Generated PDF version
- `build_prd_pdf.py` — PDF generator script

## Document Overview

The PRD provides detailed functional specifications aligned to the BRD:

- **Product Overview** — High-level product purpose and features
- **Goals & Non-Goals** — What is and isn't in scope
- **User Stories** — 8 user stories covering key personas
- **Functional Requirements** — 9 detailed FR categories:
  - FR-1: Kafka Message Consumption
  - FR-2: Payload Validation & PII Masking
  - FR-3: Message Delivery
  - FR-4: Retry & Fault Tolerance
  - FR-5: Dead Letter Queue & Alerting
  - FR-6: MongoDB Audit Storage
  - FR-7: Elasticsearch Search Index
  - FR-8: Search & Stats API
  - FR-9: Observability & Metrics
- **Non-Functional Requirements** — Performance, security, operability targets
- **Data Models** — DeliveryRecord, StatusTransition, DeliveryDocument
- **Delivery State Machine** — Visual state flow diagram
- **Error Handling Standards** — Error scenarios and recovery paths
- **Dependencies** — Software and infrastructure versions
- **Acceptance Criteria** — Testable criteria for each FR
- **Open Questions** — Issues requiring clarification

## Regenerating the PDF

To regenerate the PDF after editing the markdown:

```bash
python -u build_prd_pdf.py
```

**Prerequisites:**
- Python 3.7+
- reportlab: `pip install reportlab`

## Related Documents

- `../brd/message-relay-BRD.md` — Business Requirements Document (requirements rationale)
- `../HLD-LLD.md` — Architecture document (technical design)

