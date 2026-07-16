# Business Requirements Document (BRD)

This folder contains the **Business Requirements Document (BRD)** for the message-relay project.

## Contents

- `message-relay-BRD.md` — Source markdown file containing the complete BRD
- `message-relay-BRD.pdf` — Generated PDF version
- `build_brd_pdf.py` — PDF generator script

## Document Overview

The BRD defines what message-relay must do and why:

- **Executive Summary** — Problem statement and value proposition
- **Business Objectives** — 5 core business goals (BO-1 through BO-5)
- **Business Rules** — 6 operational rules governing delivery behavior
- **Functional Scope** — In-scope and out-of-scope features
- **Performance Requirements** — Throughput, latency, and reliability targets
- **Security & Compliance** — PII masking, credentials, audit trail requirements
- **Operational Requirements** — Monitorability, DLQ replay, graceful shutdown
- **Success Metrics & KPIs** — Delivery success rates, incident response times
- **Risk Register** — Known risks and mitigations

## Regenerating the PDF

To regenerate the PDF after editing the markdown:

```bash
python -u build_brd_pdf.py
```

**Prerequisites:**
- Python 3.7+
- reportlab: `pip install reportlab`

## Related Documents

- `../prd/message-relay-PRD.md` — Product Requirements Document (detailed implementation specs)
- `../HLD-LLD.md` — Architecture document (technical design)

