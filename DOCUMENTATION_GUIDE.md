# message-relay Documentation Structure

Generated: 2026-07-16

---

## Overview

This directory contains the complete documentation suite for the **message-relay** project. Documentation is organized by document type and includes both markdown source and PDF artifacts.

---

## Documentation Organization

### 1. Business Requirements (BRD)
**Location:** `docs/brd/`

Defines **WHAT** and **WHY** the system exists:
- Problem statement and business case
- 5 Business Objectives (BO-1 through BO-5)
- 6 Business Rules governing behavior
- Stakeholders and success metrics
- Risk register and assumptions

**Files:**
- `message-relay-BRD.md` — Source document (348 lines)
- `message-relay-BRD.pdf` — PDF export (0.02 MB)
- `build_brd_pdf.py` — PDF generator script
- `README.md` — Quick reference guide

---

### 2. Product Requirements (PRD)
**Location:** `docs/prd/`

Defines **HOW** the system will be built:
- Product overview and user stories (8 stories)
- 9 Functional Requirements (FR-1 through FR-9):
  - Kafka consumption and manual acknowledgement
  - Payload validation and PII masking
  - Message delivery strategy pattern
  - Retry and circuit breaker configuration
  - Dead letter queue and Sentry alerting
  - MongoDB audit storage with statusHistory
  - Elasticsearch search indexing
  - REST API endpoints (search, stats, detail)
  - Observability and metrics
- Non-functional requirements (performance, security, reliability)
- Delivery state machine diagram
- Acceptance criteria and open questions

**Files:**
- `message-relay-PRD.md` — Source document (742 lines)
- `message-relay-PRD.pdf` — PDF export (0.03 MB)
- `build_prd_pdf.py` — PDF generator script
- `README.md` — Quick reference guide

---

### 3. High Level Design (HLD)
**Location:** `docs/`

Defines the **architecture and system context**:
- System purpose and scope
- External dependencies (Kafka, MongoDB, Elasticsearch, Sentry, Prometheus)
- High-level architecture building blocks
- Logical component diagram (ASCII)
- Data flow end-to-end
- Deployment view and non-functional characteristics

**Files:**
- `HLD-LLD.md` — Combined HLD + LLD source (328+ lines)
- `HLD-LLD.pdf` — Combined PDF export (0.01 MB)
- `HLD.pdf` — Separate HLD-only export
- `build_hld_lld_pdf.py` — PDF generator (produces 3 PDFs)

---

### 4. Low Level Design (LLDs)
**Location:** `docs/`

Defines **component-level details and interactions**:
- LLD-1: Event ingestion and consumer layer
- LLD-2: Validation and orchestration (DeliveryService)
- LLD-3: Sender strategy and channel abstraction
- LLD-4: Data model and persistence (MongoDB + Elasticsearch)
- LLD-5: Failure handling, DLQ, and monitoring
- LLD-6: REST API layer (search/stats endpoints)
- LLD-7: Health and observability integration
- ASCII diagrams showing interaction sequence
- Traceability to source code files
- Configuration snapshot

**Files:**
- `HLD-LLD.md` — Combined source
- `LLD.pdf` — Separate LLD-only export
- Same generator as HLD

---

## Quick Access

### For Business Stakeholders
Start with: `brd/message-relay-BRD.pdf`
- Understand the problems being solved
- See the business value and KPIs
- Review assumptions and constraints

### For Product Managers
Read: `prd/message-relay-PRD.pdf`
- Understand detailed functional requirements
- Review user stories and acceptance criteria
- Check state machine and API contracts

### For Architects
Read: `HLD.pdf`
- High-level system design
- Component relationships
- Deployment context

### For Engineers
Read: `LLD.pdf` and source files
- Detailed component responsibilities
- Data models and persistence strategies
- Code organization and patterns
- Direct references to source files

---

## Regenerating PDFs

All PDFs are generated from markdown sources using ReportLab. To regenerate:

### Architecture (HLD/LLD)
```powershell
cd .\docs
python -u build_hld_lld_pdf.py
```
Produces: `HLD-LLD.pdf`, `HLD.pdf`, `LLD.pdf`

### BRD
```powershell
cd .\docs\brd
python -u build_brd_pdf.py
```
Produces: `message-relay-BRD.pdf`

### PRD
```powershell
cd .\docs\prd
python -u build_prd_pdf.py
```
Produces: `message-relay-PRD.pdf`

### All Documents (from root)
```powershell
python -m pip install reportlab
python -u .\docs\build_hld_lld_pdf.py
python -u .\docs\brd\build_brd_pdf.py
python -u .\docs\prd\build_prd_pdf.py
```

---

## Document Relationships

```
                    BRD (Business Requirements)
                    ↓ (business objectives drive)
                    ↓
                    PRD (Product Requirements)
                    ↓ (detailed spec drives)
                    ↓
                HLD (High Level Design)
                    ↓ (architecture refines)
                    ↓
                LLD (Low Level Design)
                    ↓ (components implement)
                    ↓
            Source Code (Java/Spring Boot)
```

---

## Traceability Matrix

| Req ID | Document | Section | Purpose |
|---|---|---|---|
| BO-1 to BO-5 | BRD | Business Objectives | Core business drivers |
| BR-1 to BR-6 | BRD | Business Rules | Operational constraints |
| FR-1 to FR-9 | PRD | Functional Requirements | Implementation specs |
| LLD-1 to LLD-7 | HLD/LLD | Low Level Designs | Component details |
| SC-1 to SC-5 | BRD | Security & Compliance | Security requirements |
| OR-1 to OR-5 | BRD | Operational Requirements | Operational concerns |
| US-01 to US-08 | PRD | User Stories | User needs |

---

## File Statistics

```
docs/
├── Main Architecture
│   ├── HLD-LLD.md              (328+ lines, combined)
│   ├── HLD.pdf                 (0.01 MB)
│   ├── LLD.pdf                 (0.01 MB)
│   └── HLD-LLD.pdf             (0.01 MB)
│
├── BRD (Business Requirements)
│   ├── brd/message-relay-BRD.md      (348 lines)
│   └── brd/message-relay-BRD.pdf     (0.02 MB)
│
├── PRD (Product Requirements)
│   ├── prd/message-relay-PRD.md      (742 lines)
│   └── prd/message-relay-PRD.pdf     (0.03 MB)
│
└── Total PDFs: 5
    Total Size: ~0.08 MB combined
    Total Markdown: ~1,418 lines
```

---

## Key Takeaways

1. **BRD → PRD → HLD → LLD**: Documentation flows from business needs to technical implementation
2. **Separated Organization**: BRD, PRD, and architecture are in separate folders for easy navigation
3. **Markdown + PDF**: All documents maintain markdown source for version control while exporting to PDF for distribution
4. **Reproducible Builds**: Python generator scripts allow PDFs to be regenerated after edits
5. **Complete Traceability**: All requirements, objectives, and rules are traceable through to design components

---

## Next Steps

1. **Review BRD** with business stakeholders to align on requirements
2. **Review PRD** with product and engineering to finalize functional specs
3. **Review Architecture** (HLD/LLD) with technical leads to validate design
4. **Track Requirements** using a traceability matrix (see above)
5. **Maintain Documents** by keeping markdown updated; regenerate PDFs on each release

---

Generated using ReportLab 5.0.0 | Python 3.7+

