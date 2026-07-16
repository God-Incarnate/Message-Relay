from pathlib import Path

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import Paragraph, Preformatted, SimpleDocTemplate, Spacer


ROOT = Path(__file__).resolve().parent
INPUT_MD = ROOT / "message-relay-PRD.md"
OUTPUT_PDF = ROOT / "message-relay-PRD.pdf"


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


def render_pdf():
    if not INPUT_MD.exists():
        raise FileNotFoundError(f"Markdown file not found: {INPUT_MD}")

    styles = make_styles()
    all_lines = INPUT_MD.read_text(encoding="utf-8").splitlines()

    doc = SimpleDocTemplate(
        str(OUTPUT_PDF),
        pagesize=A4,
        leftMargin=16 * mm,
        rightMargin=16 * mm,
        topMargin=14 * mm,
        bottomMargin=14 * mm,
        title="Message Relay - PRD",
        author="message-relay",
    )
    doc.build(build_story(all_lines, styles))
    print(f"Generated: {OUTPUT_PDF}")


if __name__ == "__main__":
    render_pdf()

