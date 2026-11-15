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

To run the **SQLite database edition**, install Maven and use a JDK 17 or later. Maven needs network access for dependencies on the first run unless they are already cached.

```bash
mvn compile exec:java
```

Maven adds Xerial SQLite JDBC 3.46.1.3 and SLF4J 2.0.16. Check for **SQLite** in the dashboard storage badge. If either SQLite repository fails to initialize, the app logs the failure and uses the local file repositories. The SQLite data lives in `data/campusfix.db`, while offline data uses separate files. The two modes do not synchronize.

For an isolated SQLite demonstration, use a separate copy of the checkout or run the compiled entry point from an empty working directory with an absolute classpath. Use `--empty` to skip sample seeding. To run with cached JARs and without Maven, first compile via the offline test script, then put `out`, `sqlite-jdbc-3.46.1.3.jar`, `slf4j-api-2.0.16.jar`, and `slf4j-nop-2.0.16.jar` on the classpath (`;` on Windows, `:` on Unix), and run `com.campusfix.CampusFixApp` without `--file`. Set paths for your own checkout and dependency cache.

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
- Explainable administrator technician recommendations using category-specialty fit, active workload, and deterministic tie-breaking; assignment always requires manual confirmation.
- Filterable administrator analytics with KPI cards, status/category/severity charts, creation trends, technician workload comparisons, and SLA compliance calculated from stored complaint and event data.
- Factual technician performance comparison showing active work, completed tickets, measurable average resolution time, overdue assignments, and specialization without artificial scores.
- Valid state sequence: **OPEN → ASSIGNED → IN_PROGRESS → RESOLVED**.
- Detailed ticket views with full metadata, status progression, and chronological activity timelines.
- Authorized student/admin comments, technician progress notes, and mandatory resolution notes.
- Explainable priority score out of 100 using severity, category, complaint age, and operational/SLA urgency.
- Calendar-time SLA monitoring with deadlines, remaining-time badges, due-soon warnings, overdue indicators, and reliable resolved-ticket compliance.
- Combined status, severity, category, date-range, SLA, technician, and text filters with priority, creation-date, deadline, and workflow sorting.
- JDBC SQLite persistence or versioned local-file persistence for tickets and activity events.
- Background CSV reporting for all/filtered complaints, SLA performance, and technician workload; safely quotes multiline values and neutralizes spreadsheet formulas.
- Exception handling for invalid permissions and invalid workflow transitions.

## Java concepts demonstrated

| Concept | Implementation |
| --- | --- |
| Inheritance / polymorphism | Abstract `User` and three derived role classes |
| Encapsulation | Private model fields with controlled mutation |
| Interfaces | `TicketRepository`, `TicketEventRepository`, `PriorityScorer`, `SlaPolicy`, `SpecializationMatcher`, `TicketListener` |
| Exceptions | `ValidationException`, persistence exception wrapping |
| Collections | `ArrayList`, `EnumMap`, `TreeMap`, `PriorityQueue`, streams and comparators |
| GUI and events | `JFrame`, `JTable`, Swing dialogs, listeners |
| File handling | `FileTicketRepository`, `FileTicketEventRepository`, `CsvExporter` |
| JDBC and SQLite | Ticket and event repositories, prepared statements, additive schema creation |

## Source organization

```text
src/main/java/com/campusfix/
├── CampusFixApp.java
├── model/          # User hierarchy, complaints, ticket events, enums
├── service/        # Workflow, authorization, querying, analytics, reporting, priority/SLA strategies
├── persistence/    # Ticket/event repositories for file and SQLite modes
├── ui/             # Swing dashboard, details dialog, timeline components
└── util/           # CSV exporter
src/test/java/com/campusfix/
├── SmokeTest.java  # Automated functional tests
├── TicketEventTest.java  # Activity, authorization, compatibility, persistence tests
├── OperationalIntelligenceTest.java  # Priority, SLA, recommendations, reassignment regression
├── AnalyticsReportingTest.java  # Aggregations, filters, sorting, authorization, safe CSV reports
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

With Maven and its dependencies available, `mvn test` is configured to run the same `SmokeTest` entry point with the test classpath. A direct JDBC test uses `java -ea -cp "<compiled output and dependency JARs>" com.campusfix.SmokeTest`. Tests create and reopen isolated repositories or temporary databases.

The test suite validates login, inheritance, permissions, invalid inputs, ticket creation and assignment, state transitions, comments, progress and resolution notes, chronological event retrieval, malformed-event failures, persistence reloads, legacy `v1` compatibility, weighted priority boundaries and aging, SLA deadlines and resolved compliance, specialty/workload recommendations, safe in-progress reassignment, analytics aggregation, empty datasets, missing resolution history, combined filters, date boundaries, sorting, technician metrics, report authorization, CSV escaping, and spreadsheet-injection protection. SQLite event and analytics consistency tests run automatically when the JDBC driver is on the test classpath; the offline scripts explicitly report them as `NOT VERIFIED` otherwise.

## Analytics and reports

`AnalyticsService` is the single aggregation layer for complaint totals, status/category/severity distributions, resolution rate, overdue and due-soon counts, measurable resolution duration, technician workload/completions, SLA compliance, and daily creation trends. `ComplaintQueryService` applies every table and analytics filter using the same rules. The analytics screen clearly states that all KPIs and charts reflect the selected filters.

Resolution averages include only complaints with a reliable persisted `TICKET_RESOLVED` event. SLA compliance excludes legacy resolved complaints whose outcome cannot be reconstructed. Empty or unknown percentages and durations display **Not available**, never a misleading zero.

Administrators can export:

- all complaint details;
- the currently filtered complaint set;
- an SLA performance summary; or
- a technician workload summary.

Exports use `yyyy-MM-dd HH:mm:ss` timestamps and the same priority/SLA calculations as the GUI. The filtered complaint report follows the displayed rows and order. Choose a fresh filename for each export. User-entered values beginning with `=`, `+`, `-`, or `@` are prefixed before CSV quoting to defend against spreadsheet formula execution. Authentication passwords are never part of report models.

## Explainable operational rules

Priority is deterministic and capped at 100 points. The documented maximum weights are **severity 55**, **category 15**, **age 20**, and **operational urgency 10**. Age adds one point per six complete calendar hours. Operational urgency reflects the workflow state and whether the ticket is due soon or overdue. The ticket dialog and CSV export show the same component explanation calculated by `ComplaintService`.

The default calendar-time SLA policy is:

| Severity | Target | Due-soon window |
| --- | ---: | ---: |
| CRITICAL | 4 hours | 1 hour |
| HIGH | 24 hours | 4 hours |
| MEDIUM | 72 hours | 12 hours |
| LOW | 168 hours | 24 hours |

Resolved SLA compliance is calculated only when a persisted `TICKET_RESOLVED` event supplies a reliable timestamp. Legacy resolved complaints without that event display **Compliance unavailable** instead of inventing a completion time.

Technician recommendations first rank direct category-specialty matches, then lower active workload, then technician ID for deterministic ties. If nobody has a direct specialty match, the dialog labels the choices as workload-ranked fallbacks. Administrators still choose and confirm the technician manually. Reassigning an `IN_PROGRESS` ticket preserves its status and writes a reassignment event containing the previous and new technician IDs.

The file repositories attempt a same-directory atomic replacement and fall back when it fails. Complaint state and activity are separate writes in both storage modes. If the second write fails, the service reports that the complaint changed but its activity could not be recorded.

## Demonstration in 5–10 minutes

1. Log in as `student`, submit a new maintenance issue, and show its initial OPEN status.
2. Switch to `admin` to see the new item in the queue and combine category, status, SLA, date, and technician filters.
3. Open Analytics to demonstrate filtered KPI cards, Java2D charts, trends, and honest **Not available** historical metrics.
4. Review technician performance, then select the ticket and inspect the specialty/workload recommendation before manually confirming assignment.
5. Open the full ticket view to show its status progression and assignment event.
6. Switch to `tech` or `tech2`, add a progress note, advance the ticket, and enter the required resolution note.
7. Return to `admin` to export a filtered complaint report or SLA/technician summary, then reopen the app to demonstrate persistence.
8. Explain the `User` hierarchy, repository interfaces, analytics/query services, authorization, and legacy-compatible storage.

## Submission materials

- [Editable report](docs/CampusFix_Final_Report.docx), [printable report](docs/CampusFix_Final_Report.pdf), and [report source](docs/CampusFix_Final_Report.md).
- [Final diagrams](docs/diagrams), [diagram source](docs/build_diagrams.py), and [live Swing screenshots](docs/screenshots). Earlier deliverables in `docs/` are historical versions.
- [Eight-minute demo](DEMO_SCRIPT.md), [viva questions](VIVA_QUESTIONS.md), [contribution guide](CONTRIBUTING.md), and [submission checklist](FINAL_SUBMISSION_CHECKLIST.md).

The report uses Times New Roman, 12-point body text, 14-point headings, and 1.15 spacing. Fill in the team details in the editable report before exporting the personalized PDF and printing. The Swing capture command after compilation is `java -cp out com.campusfix.CaptureScreens docs/screenshots`. It needs a graphical session and uses temporary demo data.

## Before LMS submission

- [ ] Add all four team names, roll numbers, course code, and semester to the report cover.
- [ ] Add your team members' actual contributions and verify claims.
- [ ] Confirm project starts on the laptop you will bring to demo day.
- [ ] Run the tests and take any additional screenshots you want to include.
- [ ] Create a **real** GitHub repository and upload this source (do not submit a made-up URL).
- [ ] Upload the finished PDF report plus full code archive or GitHub link by **30 Nov 2026**.
- [ ] Bring a printed copy of the report for the **4–7 Dec 2026** demo; all four members attend.

### Future improvements

Real sign-up with hashed passwords, institutional authentication, REST API, concurrent multi-file transactions, notification emails, attachments, structured technician skill records instead of free-text keyword matching, configurable SLA administration screens, monthly/weekly trend bucketing for multi-year datasets, and JDBC integration in a dependency-enabled CI build.
