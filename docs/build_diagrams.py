"""Generate four printable CampusFix diagrams with Pillow. Run from any directory."""
from pathlib import Path
from math import atan2, cos, sin
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).parent / "diagrams"
OUT.mkdir(exist_ok=True)
NAVY, TEAL, PALE, LINE = "#152842", "#007b78", "#eaf5f4", "#66788b"


def font(size):
    for name in ("arial.ttf", "DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            pass
    return ImageFont.load_default(size=size)


def canvas(height):
    image = Image.new("RGB", (1440, height), "white")
    return image, ImageDraw.Draw(image)


def box(draw, x, y, w, h, title, body="", fill=PALE):
    draw.rounded_rectangle((x, y, x+w, y+h), radius=12, fill=fill, outline="#b9cdcf", width=2)
    title_font = font(25)
    if title_font.getlength(title) > w - 18:
        title_font = font(20)
    draw.text((x+w/2, y+24), title, font=title_font, fill=NAVY, anchor="mt")
    if body:
        draw.multiline_text((x+w/2, y+62), body, font=font(21), fill=NAVY,
                            anchor="ma", align="center", spacing=9)


def arrow(draw, points, kind="dependency", label=None):
    color = TEAL if kind == "flow" else LINE
    a, b = points[-2:]
    angle = atan2(b[1]-a[1], b[0]-a[0])
    if kind in ("dependency", "implements"):
        for p, q in zip(points, points[1:]):
            length = max(1, ((q[0]-p[0])**2+(q[1]-p[1])**2)**.5)
            for step in range(0, int(length), 14):
                bounds = [step/length, min(step+8, length)/length]
                draw.line([(p[0]+(q[0]-p[0])*t, p[1]+(q[1]-p[1])*t) for t in bounds],
                          fill=color, width=3)
    else:
        draw.line(points, fill=color, width=3)
    if kind != "association":
        head = [b] + [(b[0]-16*cos(angle+offset), b[1]-16*sin(angle+offset))
                      for offset in (.5, -.5)]
        draw.polygon(head, fill="white" if kind in ("extends", "implements") else color, outline=color, width=2)
    if label:
        x, y, caption = label
        draw.text((x, y), caption, fill=NAVY, font=font(20), anchor="mt")


def save(image, name):
    image.save(OUT / (name + ".png"), optimize=True)


image, d = canvas(435)
for i, (role, actions) in enumerate([
    ("Student", "Submit a complaint\nView own tickets and history\nAdd comments\nExport visible complaints"),
    ("Administrator", "View the campus queue\nAssign or reassign work\nAdvance status and comment\nReview analytics and reports"),
    ("Technician", "View assigned tickets and history\nAdd progress notes\nStart or resolve assigned work\nExport visible complaints"),
]):
    x = 20 + i * 470
    box(d, x+40, 20, 360, 72, role, fill="#e8edf5")
    arrow(d, [(x+220, 92), (x+220, 143)], "association")
    d.ellipse((x, 144, x+440, 346), fill=PALE, outline="#b9cdcf", width=2)
    d.multiline_text((x+220, 244), actions, font=font(20), fill=NAVY,
                     anchor="mm", align="center", spacing=8)
box(d, 380, 361, 680, 63, "All roles sign in through UserDirectory", fill="#e8edf5")
save(image, "use-case")

image, d = canvas(440)
steps = [
    ("OPEN", "Student submits\na valid complaint"),
    ("ASSIGNED", "Administrator\nconfirms technician"),
    ("IN_PROGRESS", "Assigned technician\nor admin starts work"),
    ("RESOLVED", "Assigned technician or admin\nprovides a resolution note"),
]
for index, (name, detail) in enumerate(steps):
    x = 10+index*365
    box(d, x, 50, 319, 150, name, detail)
    if index < 3:
        arrow(d, [(x+319, 125), (x+365, 125)], "flow")
box(d, 345, 288, 750, 135, "Administrator reassignment",
    "ASSIGNED or IN_PROGRESS stays in its current state.\nHistory records both IDs and access follows the new assignment.")
for x in (535, 900):
    arrow(d, [(x-14, 200), (x-14, 288)], "flow")
    arrow(d, [(x+14, 288), (x+14, 200)], "flow")
save(image, "workflow")

image, d = canvas(660)
for y, title, body in [
    (15, "Swing presentation", "CampusFixFrame · ComplaintTablePanel · ComplaintDetailDialog · AnalyticsDashboardPanel"),
    (175, "Business services", "ComplaintService · ComplaintQueryService · AnalyticsService · ReportService"),
    (335, "Repository interfaces", "TicketRepository · TicketEventRepository"),
    (495, "Storage implementations", "File repositories → complaints.tsv and events.tsv\nSQLite repositories → complaints and complaint_events tables"),
]:
    box(d, 40, y, 1360, 125 if y < 495 else 150, title, body)
    if y < 495:
        arrow(d, [(720, y+125), (720, y+160)], "flow")
save(image, "architecture")

image, d = canvas(1210)
box(d, 520, 10, 400, 102, "User «abstract»", "id · name · getRole()")
for x, name in [(190, "Student"), (566, "Administrator"), (942, "Technician")]:
    box(d, x, 166, 310, 75, name)
    arrow(d, [(x+155, 166), (x+155, 138), (720, 138), (720, 112)], "extends")
box(d, 16, 292, 395, 106, "Complaint", "ticket ID · status · user IDs")
box(d, 1029, 292, 395, 106, "TicketEvent", "complaint ID · actor ID · timestamp")
arrow(d, [(411, 344), (1029, 344)], "association", (720, 311, "One complaint to zero or more events by ID"))
box(d, 520, 424, 400, 75, "ComplaintService")
arrow(d, [(520, 456), (207, 456), (207, 398)])
arrow(d, [(920, 456), (1226, 456), (1226, 398)])
for x, title, body in [
    (15, "AnalyticsService", "uses ComplaintQueryService"),
    (370, "PriorityScorer «interface»", ""),
    (725, "SlaService", "uses SlaPolicy strategy"),
    (1080, "TechnicianRecommendationService", "uses specialization matcher"),
]:
    box(d, x, 569, 345, 108, title, body)
    tip = x+172
    if x == 15:
        arrow(d, [(tip, 569), (tip, 535), (550, 535), (550, 499)])
    else:
        port = {370:620, 725:785, 1080:880}[x]
        arrow(d, [(port, 499), (port, 535), (tip, 535), (tip, 569)])
box(d, 371, 708, 343, 65, "WeightedPriorityScorer")
arrow(d, [(542, 708), (542, 677)], "implements")
box(d, 42, 843, 600, 70, "TicketRepository «interface»")
box(d, 798, 843, 600, 70, "TicketEventRepository «interface»")
for x in (342, 1098):
    arrow(d, [(720, 499), (720, 805), (x, 805), (x, 843)])
for x, title, names in [
    (42, "Complaint storage", "FileTicketRepository\nSqliteTicketRepository"),
    (798, "Event storage", "FileTicketEventRepository\nSqliteTicketEventRepository\nInMemoryTicketEventRepository"),
]:
    box(d, x, 970, 600, 175, title, names)
    arrow(d, [(x+300, 970), (x+300, 913)], "implements")
d.text((720, 1180), "Solid triangle  extends     Dashed triangle  implements     Dashed arrow  uses",
       fill=NAVY, font=font(20), anchor="mm")
save(image, "class-diagram")
