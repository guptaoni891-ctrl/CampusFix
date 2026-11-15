# CampusFix Development Guide

## Project scope

CampusFix is a Java 17-compatible university desktop application for campus maintenance complaints. Keep the existing Java Swing application and improve it incrementally; do not replace it with JavaFX, a web application, or another language.

The principal layers are:

- `model`: the `User` inheritance hierarchy, `Complaint`, and domain enums.
- `service`: validation, authorization, workflow, priority scoring, listeners, users, and demo seeding.
- `persistence`: the `TicketRepository` abstraction plus offline file and optional SQLite implementations.
- `ui`: Swing presentation and event handling.
- `util`: exports and other focused utilities.

Keep business rules out of Swing event handlers. UI code should call services; services should depend on repository interfaces rather than concrete storage classes.

## Non-negotiable rules

1. Inspect the relevant production code, tests, and stored-data format before editing.
2. Preserve working behavior and public APIs unless an intentional, documented migration is required.
3. Maintain meaningful OOP design: encapsulated domain state, the `User` hierarchy, interfaces at replaceable boundaries, polymorphism, and specific exceptions.
4. Keep offline file mode fully functional with only the JDK. SQLite/JDBC must remain optional.
5. Never describe deterministic scoring, recommendations, or rules as AI.
6. Do not add frameworks or external dependencies unless they solve a demonstrated need and offline mode still works.
7. Validate permissions and workflow transitions in the service/domain layer, not only in the GUI.
8. Add deterministic tests for every new business rule and regression.
9. Compile and run the complete automated test suite after each implementation.
10. Update README/report claims when behavior, commands, credentials, or architecture changes.

## Data safety and compatibility

- Treat `data/complaints.tsv`, `data/events.tsv`, `data/campusfix.db`, and their sidecar files as user data. Never delete, truncate, rewrite, seed over, or use them in tests.
- Tests must use a temporary directory and clean up only that exact temporary directory.
- File records currently contain 12 Base64-encoded tab-separated fields and the version marker `v1`. Preserve support for existing `v1` records when evolving the schema.
- Activity records are stored separately as seven Base64-encoded tab-separated fields in `events.tsv`, also marked `v1`; never invent events for complaints that predate activity tracking.
- SQLite schema changes must be additive or use an explicit migration. Never drop the complaints table as part of application startup.
- Keep writes atomic where practical. A failed save must not replace a valid data file with partial content.
- Complaint and event file replacements are individually safe but are not a cross-file transaction. Surface partial-write failures honestly rather than claiming atomicity.
- Do not silently fabricate missing domain values during migration; use documented defaults only when their meaning is safe and clear.

## Build and test commands

The supported baseline is JDK 17 or newer, compiling with `--release 17`.

On Windows:

```powershell
.\test.bat
.\run.bat
```

The batch scripts prefer `%JAVA_HOME%\bin` over the first Java installation on `PATH`. Ensure `JAVA_HOME` references JDK 17 or newer.

On macOS/Linux:

```bash
./test.sh
./run.sh
```

The shell scripts also prefer `$JAVA_HOME/bin` when `JAVA_HOME` is configured.

Optional SQLite mode requires Maven and the dependencies declared in `pom.xml`:

```bash
mvn test
mvn compile exec:java
```

At minimum, verification for a change must include compilation of all main and test sources plus `SmokeTest`. Persistence work must also verify a repository reload from a temporary file. If Maven is available, include SQLite integration tests for SQLite changes.

## Coding guidance

- Use clear Java formatting and descriptive names; avoid extending the existing dense one-line style in new code.
- Prefer constructor injection for repositories, scorers, clocks, and other replaceable behavior.
- Return defensive copies or immutable views across service/repository boundaries.
- Keep time-dependent logic testable by injecting a `Clock` or passing the evaluation time.
- Avoid broad `catch (RuntimeException)` in new UI code when a specific validation or persistence exception can be handled.
- Perform potentially slow persistence and export work without freezing the Swing event-dispatch thread when the operation can be noticeable.
- Keep role visibility and authorization separate: hiding a button is not a security check.
- Enforce ticket-history visibility, comment ownership, technician assignment, and mandatory resolution notes in `ComplaintService`, not in Swing components.
- Keep filtering and analytics aggregation in `ComplaintQueryService` and `AnalyticsService`; Swing charts must render service results rather than recalculate metrics.
- Treat missing resolution events as unknown history. Do not include unknown outcomes in duration or SLA-compliance denominators.
- CSV exports must use the shared priority/SLA services, quote multiline fields, and neutralize spreadsheet-formula prefixes in user-entered text.
- Run analytics, large in-memory filters, persistence refreshes, and report writes off the Swing event-dispatch thread, then update components in `SwingWorker.done()`.
- Use prepared statements for all SQL values and transactions for multi-step database changes.

## Definition of done

A change is complete when its business behavior is tested, all sources compile for Java 17, automated tests pass, offline mode remains usable, existing complaint data remains readable, and user-facing documentation matches the implementation.
