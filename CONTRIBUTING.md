# Contributing to CampusFix

Work from a shared, reviewed integration branch. Each of the four members creates their own short-lived branch, for example `team/member-1-ticket-validation`, with one purpose per branch. Coordinate before editing the same service, UI panel, or report simultaneously. Use pull requests to propose changes and ask another member to review the code and evidence.

1. Start from the current integration branch, update it, and create your topic branch with `git switch -c team/member-1-topic`. Use your own member number and topic.
2. Inspect `AGENTS.md` and the relevant tests. Preserve Java 17 compatibility and existing file formats.
3. Run `test.bat` on Windows or `./test.sh` in Bash. Run `mvn test` where Maven and SQLite JDBC are available and disclose any skipped checks. Use temporary fixtures for data and workflows.
4. Review `git status --short` and `git diff --check` before committing. Stage only the intended changes. Keep real complaint records, database files, exports, build output, personal identifiers, and any real credentials out of commits.
5. Push your topic branch only when the team is ready to share it. Open a pull request with a short summary, test results, data-format impact, and screenshots for visible changes. Merge after review and run the suite on the integrated branch.

The accounts documented in the README are public classroom fixtures. Use those accounts and synthetic records when capturing evidence. Keep the personalized report in the private submission copy if the repository will be public.
