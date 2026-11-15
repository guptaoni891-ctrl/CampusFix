# Eight Minute CampusFix Demonstration

Rehearse on the presentation laptop. Use an isolated demo working directory, bring both the editable report and the printed copy, and compile before the timer starts. Keep a clean demo data folder and one unique CSV output name ready. Assign the four speaking slots separately from claims about individual contributions.

| Clock | Speaker | Live actions and talking points |
| --- | --- | --- |
| 0:00–2:00 | Member 1 | Introduce the problem of scattered maintenance messages and unclear ownership. Open the Swing sign-in, enter the student demo credentials, and show the student's queue. Point to the shared navigation and explain what changes by role. Hand over with the complaint form open. |
| 2:00–4:00 | Member 2 | Submit an electrical issue for “Demo Lab 204” with a valid description and record its ID. Explain how `Student`, `Administrator`, and `Technician` extend `User`, and how `ComplaintService` calls the repository interfaces. Switch to admin, filter by the new ticket ID, and show the OPEN ticket and its timeline. |
| 4:00–6:00 | Member 3 | Show the score breakdown and SLA deadline. Open **Assign technician**, explain specialty, active workload, and the ID tiebreak, and confirm the choice. Sign in as the chosen technician, advance to IN_PROGRESS, add a progress note and resolve with a valid note. Mention that reassignment during work preserves status and transfers access. |
| 6:00–8:00 | Member 4 | Return to admin and show Analytics, the SLA compliance denominator, and a CSV export to a new demo filename. Point out the live storage badge. Explain the two SQLite tables, prepared statements, and verified repository reopen. Cite the offline and direct-JDBC test outcomes, state the single-process scope, and close with the ticket's final state. |

## Exact demo inputs

- Title — Demo Lab circuit trips
- Location — Demo Lab 204
- Description — The power strip trips whenever the workbench equipment is switched on.
- Category and severity — ELECTRICAL and HIGH
- Progress — Isolated the damaged power strip and tested the wall outlet.
- Resolution — Replaced the damaged power strip and tested the workbench under load.

If switching roles consumes time, keep the prepared seeded tickets available. Ticket #1001 starts in progress for the first technician, and #1006 provides a resolved example. If SQLite is unavailable during the rehearsal, use the offline launch and present the separately verified JDBC results accurately.
