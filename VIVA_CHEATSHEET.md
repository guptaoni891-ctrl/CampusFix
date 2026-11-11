# CampusFix — Java Viva Preparation (4 Team Members)

Each person should be able to demonstrate their own code **and** explain the complete complaint lifecycle.

## Questions an evaluator may ask

1. **Why CampusFix?** Campus maintenance requires a traceable workflow to move reported issues from submission to resolution.
2. **What makes it object-oriented?** The domain is modeled with `User`, `Student`, `Technician`, `Administrator`, and `Complaint`, supported by a service and repository abstraction.
3. **What is inheritance here?** `Student`, `Technician`, and `Administrator` extend the abstract `User` base class.
4. **Give a polymorphism example.** `User.getRole()` is overridden in each child class; services can accept a `User` regardless of the specific subclass.
5. **Why an abstract class instead of only an interface?** All users share concrete identity fields and behavior, while their role differs.
6. **Where is encapsulation used?** `Complaint` fields are private. Business updates occur through methods, not direct GUI field mutation.
7. **Which interfaces exist?** `TicketRepository` for interchangeable storage, `PriorityScorer` for strategy, and `TicketListener` for notifications.
8. **What is dependency inversion?** `ComplaintService` accepts `TicketRepository` and `PriorityScorer` interfaces rather than constructing a specific SQLite or file implementation internally.
9. **Why Swing?** It is included with standard Java, is event-driven, and works offline without a GUI library dependency.
10. **What is an event listener?** A callback invoked when users click a button or when the ticket service changes a complaint.
11. **How is a complaint prioritized?** A weighted combination of severity, category, and waiting time is capped at 100.
12. **Where is a PriorityQueue used?** `ComplaintService.urgentQueue()` ranks unresolved issues from most urgent to least urgent.
13. **How are technicians recommended?** Choose the technician with the fewest active assigned complaints (greedy workload balancing).
14. **What is a JDBC driver?** Software that lets Java execute SQL commands through the `java.sql` API against a particular database such as SQLite.
15. **What SQL is used?** `CREATE TABLE`, `SELECT`, and parameterized `INSERT ... ON CONFLICT DO UPDATE` for record changes.
16. **Why use PreparedStatement?** It handles values as parameters and avoids concatenating untrusted text into SQL statements.
17. **What if SQLite dependency is absent?** The program reports a fallback and uses `FileTicketRepository`, which stores data locally and retains it across restarts.
18. **Where is exception handling?** Input and role errors use `ValidationException`, and SQL/file errors are wrapped with helpful messages.
19. **Why can a student not change ticket status?** Role permissions are enforced inside `ComplaintService`, not only by hiding buttons.
20. **What is the allowed ticket lifecycle?** `OPEN → ASSIGNED → IN_PROGRESS → RESOLVED`. Invalid skips are rejected.
21. **How are reports generated?** `CsvExporter` writes records via buffered file I/O, escaping CSV fields with quotes.
22. **How is correctness verified?** `SmokeTest` covers authentication, role checks, state transitions, persistence reload, scoring and CSV export.
23. **What are the limitations?** Demo credentials, local single-machine operation, no notification integration or server synchronization, and no SQLite-specific automated integration tests.
24. **What would you improve next?** Secure login, server API, email alerts, attachments, availability-aware technician assignment, and database integration tests.

## Who should explain what?

- **Member 1:** Swing GUI (`CampusFixFrame`) and buttons, search, filters, dialogs, role-specific views.
- **Member 2:** Model classes, inheritance, `ComplaintService`, algorithm, permissions and transitions.
- **Member 3:** `TicketRepository`, SQLite JDBC, fallback file repository and `CsvExporter`.
- **Member 4:** Testing, UML, demo scenarios, error handling, architectural choices and limitations.

## Important

The live demo uses illustrative sample tickets and accounts. Do **not** claim that this version has real-time notifications, machine learning, multi-device synchronization, or production authentication.
