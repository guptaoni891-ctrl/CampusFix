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

These scripts compile and launch the **fully working offline file-storage mode** with no external libraries. The first launch automatically creates six sample tickets in `data/complaints.tsv`. In this mode, all new changes survive restarts.

To run the **SQLite database edition** (requires **Apache Maven** and an Internet connection for the first download of dependencies):

```bash
mvn compile exec:java
```

Maven adds Xerial SQLite JDBC, so complaint records persist in `data/campusfix.db`. The app attempts SQLite first and shows the storage mode at the bottom of the dashboard. If the JDBC driver is missing, it falls back to local file storage and logs the reason. The two storage modes use separate persistent data files.

From an IDE, open `pom.xml` as a Maven project and run `com.campusfix.CampusFixApp`.

> No JDK installation is bundled. If `javac` is not recognized, install JDK 17 or later and add it to PATH. On Windows, run these commands from the project directory rather than double-clicking the `java` file.

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

- Role-specific login, dashboards, and visibility restrictions.
- Student complaint submission with title, location, description, category, and severity.
- Administrator assignment of technicians with least-active-ticket recommendation.
- Valid state sequence: **OPEN → ASSIGNED → IN_PROGRESS → RESOLVED**.
- Priority score out of 100, based on severity, issue category, and waiting time.
- Table search, status filters, resolved-ticket counts, and outstanding-work queue.
- JDBC SQLite persistence or atomic local-file persistence.
- CSV export, correctly quoting commas, quotes, and line breaks.
- Exception handling for invalid permissions and invalid workflow transitions.

## Java concepts demonstrated

| Concept | Implementation |
| --- | --- |
| Inheritance / polymorphism | Abstract `User` and three derived role classes |
| Encapsulation | Private model fields with controlled mutation |
| Interfaces | `TicketRepository`, `PriorityScorer`, `TicketListener` |
| Exceptions | `ValidationException`, persistence exception wrapping |
| Collections | `ArrayList`, `Map`, `PriorityQueue`, streams |
| GUI and events | `JFrame`, `JTable`, Swing dialogs, listeners |
| File handling | `FileTicketRepository`, `CsvExporter` |
| JDBC and SQLite | `SqliteTicketRepository`, prepared statements, schema creation |

## Source organization

```text
src/main/java/com/campusfix/
├── CampusFixApp.java
├── model/          # User hierarchy, Complaint, enums
├── service/        # Authentication, priority scores, permissions, seeding
├── persistence/    # Repository interface, SQLite and file implementations
├── ui/             # Swing desktop interface
└── util/           # CSV exporter
src/test/java/com/campusfix/
├── SmokeTest.java  # Automated functional tests
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

The test suite validates login, inheritance, permissions, invalid inputs, ticket creation and assignment, state-transition rules, persistence across repository reload, priority scoring, and CSV export. **SQLite-specific integration tests are not included**; this would be a valuable extension for the team.

## Demonstration in 5–10 minutes

1. Log in as `student`, submit a new maintenance issue, and show its initial OPEN status.
2. Switch to `admin` to see the new item in the global queue. Explain the priority score.
3. Select the row, assign a technician, and check the updated status.
4. Switch to `tech` or `tech2` as appropriate. Advance the assigned ticket to IN_PROGRESS and RESOLVED.
5. Switch to `admin` again to show dashboard totals and CSV export. Reopen the app to demonstrate persistence.
6. Explain UML diagram, abstract `User` class, interfaces, exception handling, and database repository.

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

Real sign-up with hashed passwords, institutional authentication, REST API, concurrent access, notification emails, attachments, technician specializations, and full automated JDBC integration tests.
