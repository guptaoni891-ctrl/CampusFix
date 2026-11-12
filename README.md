# CampusFix — Smart Campus Maintenance Complaint Management System

**Java OOP coursework | Four-person team | Submission deadline: 30 November 2026**

A desktop campus maintenance tracker built with Java Swing. Students create and track complaints, administrators prioritize and assign jobs, and technicians advance work orders to resolution. Includes a real database access implementation (SQLite/JDBC), an offline file-storage fallback, CSV export, UML diagrams, example records, and a print-ready report.

## Start quickly (requires JDK 17+)

On **Windows**, open this folder in Terminal / PowerShell and run:

```powershell
.\run.bat
```

On **macOS or Linux**, run:

```bash
./run.sh
```

These scripts compile and launch the **fully working offline file-storage mode** with no external libraries. The first launch automatically creates six sample tickets in `data/complaints.tsv`. Complaint activity is stored separately in `data/events.tsv`, preserving compatibility with existing 12-field `v1` complaint records. In this mode, all new changes survive restarts.

To run the **SQLite database edition** (requires **Apache Maven** and an Internet connection for the first download of dependencies):

```bash
mvn compile exec:java
```

Maven adds Xerial SQLite JDBC, so complaint records and the additive `complaint_events` table persist in `data/campusfix.db`. The app attempts SQLite first and shows the storage mode at the bottom of the dashboard. If either SQLite repository is unavailable, it falls back to local file storage and logs the reason. The two storage modes use separate persistent data files.

From an IDE, open `pom.xml` as a Maven project and run `com.campusfix.CampusFixApp`.

> No JDK installation is bundled. The scripts prefer the JDK configured by `JAVA_HOME`, then fall back to `java` and `javac` on `PATH`. Ensure the selected JDK is version 17 or later. On Windows, run these commands from the project directory rather than double-clicking the `java` file.

## Demo sign-in

| Role | Username | Password |
| --- | --- | --- |
| Administrator | `admin` | `demo123` |
| Student 1 | `student` | `demo123` |
| Student 2 | `student2` | `demo123` |
| Technician 1 | `tech` | `demo123` |
| Technician 2 | `tech2` | `demo123` |

**Important:** These are intentionally simple **demonstration-only** accounts defined in `UserDirectory`. CampusFix v1 does not implement production-grade authentication or multi-user network synchronization. Do not store private real-world information in this classroom build.

## Core features

- Polished role-specific dashboards with sidebar navigation, responsive cards, status badges, and consistent native Swing styling.
- Student complaint submission with title, location, description, category, and severity.
- Administrator assignment of technicians with least-active-ticket recommendation.
- Live administrator analytics and technician workload views calculated from stored complaint data.
- Valid state sequence: **OPEN → ASSIGNED → IN_PROGRESS → RESOLVED**.
- Detailed ticket views with full metadata, status progression, and chronological activity timelines.
- Authorized student/admin comments, technician progress notes, and mandatory resolution notes.
- Priority score out of 100, based on severity, issue category, and waiting time.
- Table search, status filters, resolved-ticket counts, and outstanding-work queue.
- JDBC SQLite persistence or versioned local-file persistence for tickets and activity events.
- CSV export, correctly quoting commas, quotes, and line breaks.
- Exception handling for invalid permissions and invalid workflow transitions.

## Java concepts demonstrated

| Concept | Implementation |
| --- | --- |
| Inheritance / polymorphism | Abstract `User` and three derived role classes |
| Encapsulation | Private model fields with controlled mutation |
| Interfaces | `TicketRepository`, `TicketEventRepository`, `PriorityScorer`, `TicketListener` |
| Exceptions | `ValidationException`, persistence exception wrapping |
| Collections | `ArrayList`, `Map`, `PriorityQueue`, streams |
| GUI and events | `JFrame`, `JTable`, Swing dialogs, listeners |
| File handling | `FileTicketRepository`, `FileTicketEventRepository`, `CsvExporter` |
| JDBC and SQLite | Ticket and event repositories, prepared statements, additive schema creation |

## Source organization

```text
src/main/java/com/campusfix/
├── CampusFixApp.java
├── model/          # User hierarchy, complaints, ticket events, enums
├── service/        # Authentication, workflow, history authorization, scoring
├── persistence/    # Ticket/event repositories for file and SQLite modes
├── ui/             # Swing dashboard, details dialog, timeline components
└── util/           # CSV exporter
src/test/java/com/campusfix/
├── SmokeTest.java  # Automated functional tests
├── TicketEventTest.java  # Activity, authorization, compatibility, persistence tests
└── CaptureScreens.java  # Real-GUI screenshot capture utility
```

## Test

Linux / macOS:

```bash
./test.sh
```

Windows:

```powershell
.\test.bat
```

The test suite validates login, inheritance, permissions, invalid inputs, ticket creation and assignment, state transitions, comments, progress and resolution notes, chronological event retrieval, malformed-event failures, persistence reloads, legacy `v1` compatibility, priority scoring, and CSV export. SQLite event integration runs automatically when the JDBC driver is on the test classpath; the offline scripts explicitly report it as `NOT VERIFIED` otherwise.

The offline complaint file and activity file are each replaced safely, but updating a complaint and appending its event are two separate writes and are not presented as a cross-file transaction. If the second write fails, the service reports that the complaint changed but its activity could not be recorded.

## Demonstration in 5–10 minutes

1. Log in as `student`, submit a new maintenance issue, and show its initial OPEN status.
2. Switch to `admin` to see the new item in the global queue. Explain the priority score.
3. Select the row, assign a technician, and check the updated status.
4. Open the full ticket view to show its status progression and assignment event.
5. Switch to `tech` or `tech2`, add a progress note, advance the ticket, and enter the required resolution note.
6. Switch to `admin` again to review the complete activity timeline, dashboard totals, and CSV export. Reopen the app to demonstrate persistence.
7. Explain the `User` hierarchy, ticket/event repository interfaces, service authorization, and legacy-compatible storage.

## Report and diagrams

- `docs/CampusFix_Project_Report.pdf` — printable course report (15 pages).
- `docs/CampusFix_Project_Report.docx` — editable report.
- `docs/use_case.png`, `docs/flowchart.png`, `docs/class_diagram.png` — design diagrams.
- `docs/admin_dashboard.png`, `docs/student_dashboard.png` — **actual captures of the running Swing GUI**, not mockups.

The report was formatted with Times New Roman 12-point body text, 14-point headings, and 1.15 line spacing (or local font substitution if Times New Roman is unavailable).

## Before LMS submission

- [ ] Add all four team names, roll numbers, course code, and semester to the report cover.
- [ ] Add your team members' actual contributions and verify claims.
- [ ] Confirm project starts on the laptop you will bring to demo day.
- [ ] Run the tests and take any additional screenshots you want to include.
- [ ] Create a **real** GitHub repository and upload this source (do not submit a made-up URL).
- [ ] Upload the finished PDF report plus full code archive or GitHub link by **30 Nov 2026**.
- [ ] Bring a printed copy of the report for the **4–7 Dec 2026** demo; all four members attend.

### Future improvements

Real sign-up with hashed passwords, institutional authentication, REST API, concurrent multi-file transactions, notification emails, attachments, technician specializations, and JDBC integration in a dependency-enabled CI build.
