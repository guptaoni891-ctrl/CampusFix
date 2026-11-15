# CampusFix Final Submission Checklist

Verification date: 15 November 2026. Submission deadline: 30 November 2026. Demonstration window: 4–7 December 2026.

| Item | Result | Evidence or remaining action |
| --- | --- | --- |
| Code compiles | PASS | Windows and Git Bash scripts compiled all sources with JDK 21.0.13 and `--release 17`. `javap` reported class-file major version 61. |
| Automated tests | PASS | `SmokeTest`, `TicketEventTest`, `OperationalIntelligenceTest`, and `AnalyticsReportingTest` completed in each applicable run. |
| Swing GUI | PASS | `CaptureScreens` opened the running Swing frame, changed role views, selected a ticket, and opened the detail, recommendations, analytics, and report controls. |
| All three roles function | PASS | Service tests exercised student sign-in and ticket creation, admin assignment and reassignment, technician progress and resolution, and access denial. The UI capture exercised navigation and representative dialogs for all three roles. |
| Full manual GUI workflow and demo-laptop rehearsal | NOT VERIFIED | Room setup, login by typing, end-to-end GUI submission and resolution, and finishing an export through the GUI remain rehearsal checks. Automated tests cover the corresponding service operations and actual CSV writes. |
| SQLite and JDBC | PASS | Xerial 3.46.1.3 initialized a temporary database. The test persisted a lifecycle with reassignment, denied the old assignee, reopened both repositories, and recovered the final complaint state and ordered events. Analytics snapshots matched across both backends. |
| Maven execution | NOT VERIFIED | `mvn` was unavailable on PATH. Direct JDBC integration ran with cached dependencies. |
| Offline file persistence and v1 compatibility | PASS | Tests parsed a legacy complaint, rejected malformed event data, and reopened temporary complaint and event files. The production complaint hash below matches the pre-check. |
| System diagrams | PASS | Four printable diagrams in [docs/diagrams](docs/diagrams) follow the class hierarchy, service/repository dependencies, role checks, and transition rules in the source. |
| Report content and formatting | PASS | The 13-page PDF and Word-rendered DOCX cover the required sections. Both use Times New Roman, 12-point body, 14-point headings, and 1.15 spacing. |
| Genuine GUI screenshots | PASS | Eleven PNGs in [docs/screenshots](docs/screenshots) come from the live Swing test harness. The detail dialog supplies two additional crops for the SLA and timeline views. |
| GitHub preparation | PASS | README and CONTRIBUTING describe setup and review. Git ignores generated output, runtime stores, JARs, exports, and common local environment files. The tracked runtime-path audit found only `data/.gitkeep`. Demo accounts are public fixtures. |
| Team GitHub repository and LMS submission | NOT VERIFIED | The team must create or confirm its repository, review and publish the intended changes, and submit through the LMS. |
| Names, roll numbers, course, and semester | NOT VERIFIED | The supplied team-details file and report cover have placeholders. Fill in the confirmed details in the private submission copy. |
| Printed report | NOT VERIFIED | Export the completed cover to PDF, print it, and bring the copy to the demonstration. |
| Four-person demo script | PASS | [DEMO_SCRIPT.md](DEMO_SCRIPT.md) assigns four consecutive two-minute slots with a single ticket and a fallback. |
| Individual viva preparation | PASS | [VIVA_QUESTIONS.md](VIVA_QUESTIONS.md) includes 24 questions with code-specific sample answers. Individual rehearsal remains with each student. |

## Commands and results

Run from the project root on the verification workstation:

```powershell
.\test.bat
& 'C:/Program Files/Git/bin/bash.exe' -lc 'export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot"; ./test.sh'

$repo = Join-Path $env:USERPROFILE '.m2/repository'
$cp = @('out',
  "$repo/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar",
  "$repo/org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar",
  "$repo/org/slf4j/slf4j-nop/2.0.16/slf4j-nop-2.0.16.jar") -join ';'
& (Join-Path $env:JAVA_HOME 'bin/java.exe') -ea -cp $cp com.campusfix.SmokeTest
& (Join-Path $env:JAVA_HOME 'bin/java.exe') -cp out com.campusfix.CaptureScreens docs/screenshots
& (Join-Path $env:JAVA_HOME 'bin/javap.exe') -verbose -classpath out com.campusfix.CampusFixApp | Select-String 'major version'
Get-Command mvn -ErrorAction SilentlyContinue
git diff --check HEAD
git ls-files data out target
Get-FileHash -Algorithm SHA256 -LiteralPath 'data/complaints.tsv'
Test-Path -LiteralPath 'data/events.tsv'
```

Both offline scripts passed, with an explicit skip for JDBC. The direct SQLite run passed without a file fallback. The Swing harness passed. The Maven availability check returned no executable. The complaint file retains SHA-256 `F98D7E949176ADA1B4C621EDA6F3941A7F6D3B1BE06CD32A094595E43B58A701`. The production events file was absent at both checks.

## Recommendation

**GO for code and demo rehearsal. NO-GO for final university submission** until the team confirms the cover details, rehearses on its demonstration laptop, prepares the printed copy, and completes the repository and LMS handoff.

## Prompt 6 change inventory

| Area | Files |
| --- | --- |
| Configuration and repository guide | `.gitignore`, `pom.xml`, `README.md`, `CONTRIBUTING.md` |
| Application source | `FileTicketRepository.java`, `ComplaintQueryService.java`, `ComplaintService.java`, `ReportService.java`, `SlaService.java`, `UserDirectory.java`, `CampusFixFrame.java`, `ComplaintTablePanel.java`, `CsvExporter.java` |
| Verification source | `SmokeTest.java`, `TicketEventTest.java`, `AnalyticsReportingTest.java`, `CaptureScreens.java` |
| Report | `docs/CampusFix_Final_Report.md`, `docs/CampusFix_Final_Report.docx`, `docs/CampusFix_Final_Report.pdf`, `docs/build_report.py` |
| Figures | `docs/build_diagrams.py`, [four diagram PNGs](docs/diagrams), [eleven Swing PNGs](docs/screenshots) |
| Submission materials | `DEMO_SCRIPT.md`, `VIVA_QUESTIONS.md`, `FINAL_SUBMISSION_CHECKLIST.md` |
