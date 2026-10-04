# MicroLend — AI Usage Log

Required by the joint-project guidelines §9. Maintained continuously and submitted with every milestone.

**Tool used throughout:** Claude Opus 5 (Claude Code). **All Milestone 1 entries dated 4 October 2026.**

## Milestone 1

| # | Purpose | Material adopted | Modifications made | Verification |
|---|---|---|---|---|
| 1 | Compare candidate project ideas against both rubrics | Comparative analysis; MicroLend chosen over two alternatives | Scope cut to a locally-run application with no deployment or paid services | Each rubric minimum traced to where it is met; team accepted the choice |
| 2 | Draft the SE423 document | Class model, SOLID analysis, pattern justifications | Diagram split into three views for legibility; CLI replaced by a local web interface | Found and corrected a Simple Factory presented as a Factory Method |
| 3 | Draft the SE431 document | Quality requirements, RTM, SQAP outline, CoQ, risk register | Four items reclassified from quality requirements to constraints | Targets and effort estimates checked as realistic for a three-person team |
| 4 | Repository setup and coding standard | Checkstyle config, CODEOWNERS, PR template, branch rules | C-01 check rewritten as a token rule after a false positive | Branch protection tested by a direct push, which was blocked and logged |
| 5 | Business Rules Register | 43 numbered rules with boundary values | Numeric thresholds reviewed and accepted unchanged by the team | Boundary values cross-checked against the RTM test design |
| 6 | Build and CI scaffold | Gradle build, `Money`, `MoneyTest`, GitHub Actions workflow | Written as a real TDD cycle, red commit before green | CI green: compile, 10 tests, Checkstyle, coverage |
| 7 | Submission PDFs | Build script and title page | Fonts changed to carry mathematical symbols | Pages inspected; a fault dropping `≤` and `×` was found and fixed |

## Milestone 2

*(added as work proceeds)*

## Milestone 3

*(added as work proceeds)*

## Declaration for Milestone 1

> I confirm that I have read, understood and verified the material submitted under my name, that I can explain and defend the design decisions, quality requirements and analysis it contains, and that any AI-assisted content was reviewed and modified by me where needed.

**Muhammad Ibrahim — 2023446** · `origination`, `delinquency`; SQAP outline, risk register

> Signature: ............................................................  Date: ..............................

**Hassan Khalid — 2023242** · `products`, `reporting`; quality requirements, RTM

> Signature: ............................................................  Date: ..............................

**Tughral Hussain — 2023532** · `ledger`; standards awareness, Cost of Quality, repository setup

> Signature: ............................................................  Date: ..............................

## Team rules for AI use

1. Generated code enters the repository only through the module owner, who reviews it and answers for it.
2. No generated test is accepted without reading what it asserts.
3. Business rules and numeric thresholds are the team's decisions, not defaults.
4. Entries are added when the work happens, not before a deadline.
5. Anything a member cannot explain is rewritten or withdrawn before submission.
