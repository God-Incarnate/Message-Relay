from pathlib import Path

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import Paragraph, Preformatted, SimpleDocTemplate, Spacer


ROOT = Path(__file__).resolve().parent
INPUT_MD = ROOT / "HLD-LLD.md"
OUTPUT_ALL = ROOT / "HLD-LLD.pdf"
OUTPUT_HLD = ROOT / "HLD.pdf"
OUTPUT_LLD = ROOT / "LLD.pdf"


def make_styles():
    styles = getSampleStyleSheet()
    styles["Title"].fontName = "Helvetica-Bold"
    styles["Title"].fontSize = 18
    styles["Heading2"].fontSize = 13
    styles["Heading3"].fontSize = 11
    styles["BodyText"].fontSize = 9.5
    styles["BodyText"].leading = 13

    styles.add(
        ParagraphStyle(
            name="Mono",
            parent=styles["BodyText"],
            fontName="Courier",
            fontSize=8.2,
            leading=10,
        )
    )
    return styles


def markdown_line_to_para(line: str, styles):
    text = line.strip()
    if not text:
        return Spacer(1, 4)

    if text.startswith("# "):
        return Paragraph(text[2:].strip(), styles["Title"])
    if text.startswith("## "):
        return Paragraph(text[3:].strip(), styles["Heading2"])
    if text.startswith("### "):
        return Paragraph(text[4:].strip(), styles["Heading3"])
    if text.startswith("- "):
        return Paragraph(f"&bull; {text[2:].strip()}", styles["BodyText"])
    return Paragraph(text, styles["BodyText"])


def build_story(lines, styles):
    story = []
    in_code = False
    code_lines = []

    for raw_line in lines:
        if raw_line.strip().startswith("```"):
            if in_code:
                story.append(Preformatted("\n".join(code_lines), styles["Mono"]))
                story.append(Spacer(1, 4))
                code_lines = []
                in_code = False
            else:
                in_code = True
            continue

        if in_code:
            code_lines.append(raw_line)
            continue

        story.append(markdown_line_to_para(raw_line, styles))

    if code_lines:
        story.append(Preformatted("\n".join(code_lines), styles["Mono"]))

    return story


def extract_sections(all_lines):
    hld_lines = []
    lld_lines = []
    in_hld = False
    in_lld = False

    for line in all_lines:
        if line.startswith("## 1. High Level Design"):
            in_hld = True
            in_lld = False
        elif line.startswith("## 2. Low Level Design"):
            in_hld = False
            in_lld = True
        elif line.startswith("## 3. Traceability"):
            in_hld = False
            in_lld = False

        if in_hld:
            hld_lines.append(line)
        if in_lld:
            lld_lines.append(line)

    # Add top headings for split documents.
    hld_lines = ["# Message Relay - HLD", ""] + hld_lines
    lld_lines = ["# Message Relay - LLD", ""] + lld_lines
    return hld_lines, lld_lines


def render_pdf(output_path: Path, title: str, lines, styles):
    doc = SimpleDocTemplate(
        str(output_path),
        pagesize=A4,
        leftMargin=16 * mm,
        rightMargin=16 * mm,
        topMargin=14 * mm,
        bottomMargin=14 * mm,
        title=title,
        author="message-relay",
    )
    doc.build(build_story(lines, styles))
    print(f"Generated: {output_path}")


def build_pdf_set():
    if not INPUT_MD.exists():
        raise FileNotFoundError(f"Markdown file not found: {INPUT_MD}")

    styles = make_styles()
    all_lines = INPUT_MD.read_text(encoding="utf-8").splitlines()
    hld_lines, lld_lines = extract_sections(all_lines)

    render_pdf(OUTPUT_ALL, "Message Relay - HLD and LLD", all_lines, styles)
    render_pdf(OUTPUT_HLD, "Message Relay - HLD", hld_lines, styles)
    render_pdf(OUTPUT_LLD, "Message Relay - LLD", lld_lines, styles)


if __name__ == "__main__":
    build_pdf_set()


