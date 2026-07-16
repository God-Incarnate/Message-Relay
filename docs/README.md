# Architecture PDF Generator

This folder contains the architecture source and a tiny PDF exporter.

## Files
- `HLD-LLD.md`: master architecture document (HLD + LLD).
- `build_hld_lld_pdf.py`: generator that exports:
  - `HLD-LLD.pdf`
  - `HLD.pdf`
  - `LLD.pdf`
- `requirements-pdf.txt`: Python dependency list for the exporter.

## Directory Structure

```
docs/
├── README.md                 (this file)
├── requirements-pdf.txt      (Python dependencies)
├── HLD-LLD.md               (Combined architecture document)
├── HLD-LLD.pdf              (Combined architecture PDF)
├── HLD.pdf                  (Separate HLD)
├── LLD.pdf                  (Separate LLD)
├── build_hld_lld_pdf.py     (Architecture PDF generator)
│
├── brd/
│   ├── README.md                          (BRD overview)
│   ├── message-relay-BRD.md               (Business Requirements markdown)
│   ├── message-relay-BRD.pdf              (BRD PDF)
│   └── build_brd_pdf.py                   (PDF generator)
│
├── prd/
│   ├── README.md                          (PRD overview)
│   ├── message-relay-PRD.md               (Product Requirements markdown)
│   ├── message-relay-PRD.pdf              (PRD PDF)
│   └── build_prd_pdf.py                   (PDF generator)
│
└── (archived)
    └── message-relay-BRD.md               (Original in root — moved to brd/)
    └── message-relay-PRD.md               (Original in root — moved to prd/)
```

## Quick Start (PowerShell)
```powershell
python -m ensurepip --upgrade
python -m pip install -r .\docs\requirements-pdf.txt
python -u .\docs\build_hld_lld_pdf.py
python -u .\docs\brd\build_brd_pdf.py
python -u .\docs\prd\build_prd_pdf.py
python -u .\docs\build_hld_lld_pdf.py
```

## Notes
- Fenced code blocks in markdown are rendered in monospace to keep ASCII diagrams readable.
- The split PDFs are generated from sections `## 1. High Level Design (HLD)` and `## 2. Low Level Design (LLDs)`.
- BRD, PRD, and architecture documents are organized into separate subfolders for better navigation.
- All PDF generators use ReportLab and support the same lightweight markdown syntax (headings, bullets, code blocks).

