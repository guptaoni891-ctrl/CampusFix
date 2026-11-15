"""Build the editable and printable report from the shared Markdown and real figures."""
from pathlib import Path
import argparse
import re

DOCS = Path(__file__).resolve().parent
STEM = "CampusFix_Final_Report"
PICTURE = re.compile(r"!\[(.+)\]\((.+)\)")


def blocks():
    source = (DOCS / f"{STEM}.md").read_text(encoding="utf-8")
    separators = r"\n\s*\n|(?<=<!-- page -->)\n"
    return (block.strip() for block in re.split(separators, source) if block.strip())


def picture_size(path):
    from PIL import Image
    with Image.open(path) as image:
        width, height = image.size
    limit = 2.05 if path.name.startswith(("10-", "11-")) else 3.75
    if path.parent.name == "diagrams":
        limit = 5.6
    if path.name == "09-analytics.png":
        limit = 3.0
    inches = min(6.65, width / height * limit)
    return inches, inches * height / width


def clean(text):
    return text.replace("`", "")


def make_docx():
    from docx import Document
    from docx.enum.style import WD_STYLE_TYPE
    from docx.enum.text import WD_ALIGN_PARAGRAPH
    from docx.oxml import OxmlElement
    from docx.oxml.ns import qn
    from docx.shared import Inches, Pt, RGBColor

    doc = Document()
    section = doc.sections[0]
    section.page_width, section.page_height = Inches(8.27), Inches(11.69)
    section.left_margin = section.right_margin = Inches(.8)
    section.top_margin = section.bottom_margin = Inches(.75)
    for style in doc.styles:
        if style.type in (WD_STYLE_TYPE.PARAGRAPH, WD_STYLE_TYPE.CHARACTER):
            style.font.name = "Times New Roman"
            style.font.size = Pt(12)
            style.font.color.rgb = RGBColor(0, 0, 0)
            fonts = style.element.get_or_add_rPr().rFonts
            for attr in ("asciiTheme", "hAnsiTheme", "eastAsiaTheme", "cstheme"):
                fonts.attrib.pop(qn(f"w:{attr}"), None)
            fonts.set(qn("w:eastAsia"), "Times New Roman")
            fonts.set(qn("w:cs"), "Times New Roman")
    for name in ("Normal", "List Bullet", "Caption"):
        fmt = doc.styles[name].paragraph_format
        fmt.line_spacing = 1.15
        fmt.space_after = Pt(7)
    doc.styles["Caption"].font.italic = True
    for name in ("Heading 1", "Heading 2", "Title"):
        style = doc.styles[name]
        style.font.size = Pt(26 if name == "Title" else 14)
        style.font.bold = True
        style.paragraph_format.line_spacing = 1.15
        style.paragraph_format.space_before = Pt(8)
        style.paragraph_format.space_after = Pt(6)
        style.paragraph_format.keep_with_next = True
        props = style.element.get_or_add_pPr()
        border = props.find(qn("w:pBdr"))
        if border is not None:
            props.remove(border)
    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    footer.add_run("CampusFix  |  ")
    field = OxmlElement("w:fldSimple")
    field.set(qn("w:instr"), "PAGE")
    footer._p.append(field)

    cover = True
    first = True
    for block in blocks():
        if block == "<!-- page -->":
            doc.add_page_break()
            cover = False
        elif (match := PICTURE.fullmatch(block)):
            caption, image = match.groups()
            path = DOCS / image
            width, _ = picture_size(path)
            figure = doc.add_paragraph()
            figure.alignment = WD_ALIGN_PARAGRAPH.CENTER
            figure.paragraph_format.keep_with_next = True
            figure.add_run().add_picture(str(path), width=Inches(width))
            doc.add_paragraph(caption, style="Caption").alignment = WD_ALIGN_PARAGRAPH.CENTER
        elif block.startswith("#"):
            level, value = block.split(" ", 1)
            item = doc.add_paragraph(value, style={"#":"Title", "##":"Heading 1", "###":"Heading 2"}[level])
            if first:
                item.paragraph_format.space_before = Pt(105)
        else:
            for line in block.splitlines():
                item = doc.add_paragraph(clean(line.removeprefix("- ")),
                                         style="List Bullet" if line.startswith("- ") else "Normal")
                if cover:
                    item.alignment = WD_ALIGN_PARAGRAPH.CENTER
        if cover:
            doc.paragraphs[-1].alignment = WD_ALIGN_PARAGRAPH.CENTER
        first = False
    doc.core_properties.title = "CampusFix University Java OOP Project Report"
    doc.core_properties.author = "CampusFix project team"
    doc.save(DOCS / f"{STEM}.docx")


def make_pdf(font_dir):
    from html import escape
    from reportlab.lib.enums import TA_CENTER
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.styles import ParagraphStyle
    from reportlab.lib.units import inch
    from reportlab.pdfbase import pdfmetrics
    from reportlab.pdfbase.ttfonts import TTFont
    from reportlab.platypus import Image, KeepTogether, PageBreak, Paragraph, SimpleDocTemplate, Spacer

    for name, file in (("TNR", "times.ttf"), ("TNR-Bold", "timesbd.ttf"), ("TNR-Italic", "timesi.ttf")):
        pdfmetrics.registerFont(TTFont(name, str(font_dir / file)))
    normal = ParagraphStyle("Normal", fontName="TNR", fontSize=12, leading=13.8, spaceAfter=7)
    center = ParagraphStyle("Center", parent=normal, alignment=TA_CENTER)
    heading = ParagraphStyle("Heading", parent=normal, fontName="TNR-Bold", fontSize=14,
                             leading=16.1, spaceBefore=8, spaceAfter=6, keepWithNext=True)
    centered_heading = ParagraphStyle("CenteredHeading", parent=heading, alignment=TA_CENTER)
    title = ParagraphStyle("Title", parent=centered_heading, fontSize=26, leading=30)
    caption_style = ParagraphStyle("Caption", parent=center, fontName="TNR-Italic", spaceBefore=4)
    bullet = ParagraphStyle("Bullet", parent=normal, leftIndent=15, firstLineIndent=-10)
    story = []
    cover = True
    for block in blocks():
        if block == "<!-- page -->":
            story.append(PageBreak())
            cover = False
        elif (match := PICTURE.fullmatch(block)):
            caption, image = match.groups()
            path = DOCS / image
            width, height = picture_size(path)
            story.append(KeepTogether([Image(path, width * inch, height * inch),
                                       Paragraph(escape(caption), caption_style)]))
        elif block.startswith("#"):
            level, name = block.split(" ", 1)
            if level == "#":
                story.append(Spacer(1, 105))
            story.append(Paragraph(escape(name), title if level == "#" else centered_heading if cover else heading))
        else:
            for line in block.splitlines():
                if line.startswith("- "):
                    story.append(Paragraph("• " + escape(clean(line[2:])), bullet))
                else:
                    story.append(Paragraph(escape(clean(line)), center if cover else normal))

    def footer(canvas, doc):
        canvas.setFont("TNR", 10)
        canvas.drawCentredString(A4[0] / 2, 31, f"CampusFix  |  {doc.page}")

    report = SimpleDocTemplate(str(DOCS / f"{STEM}.pdf"), pagesize=A4,
        rightMargin=.8*inch, leftMargin=.8*inch, topMargin=.75*inch, bottomMargin=.75*inch,
        title="CampusFix University Java OOP Project Report", author="CampusFix project team")
    report.build(story, onFirstPage=footer, onLaterPages=footer)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("format", choices=["docx", "pdf"])
    parser.add_argument("--font-dir", type=Path, help="Times New Roman TTF directory for PDF")
    args = parser.parse_args()
    if args.format == "docx":
        make_docx()
    elif args.font_dir is None:
        parser.error("--font-dir is required for PDF")
    else:
        make_pdf(args.font_dir)
