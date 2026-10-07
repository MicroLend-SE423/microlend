# MicroLend — AI Usage Log (Milestone 1)

Required by the joint-project guidelines §9. Milestones 2 and 3 carry their own logs.

Every entry used Claude through Claude Code. Each entry is dated the day the work was done, which the repository's commit history corroborates.

| # | Date | Tool | Purpose | Prompt / activity summary | Material adopted | Modifications made | Verification |
|-|----|----|----|-----|-----|-----|-----|
| 1 | 2026-10-04 | Claude Opus 5 | Compare candidate project ideas against both rubrics | Asked for candidate project ideas scored against the §3.1 and §3.2 minimum requirements | Comparative analysis; MicroLend chosen over two alternatives | Scope cut to a locally-run application with no deployment or paid services | Each rubric minimum traced to where it is met; team accepted the choice |
| 2 | 2026-10-04 | Claude Opus 5 | Draft the SE423 document | Asked for a class model, SOLID analysis and pattern justifications from the module list and business rules | Class model, SOLID analysis, pattern justifications | Diagram split into views for legibility; CLI replaced by a local web interface | Found and corrected a Simple Factory presented as a Factory Method |
| 3 | 2026-10-04 | Claude Opus 5 | Draft the SE431 document | Asked for ISO 25010 quality requirements, an RTM, an IEEE 730 outline, a CoQ estimate and a risk register for the SE423 design | Quality requirements, RTM, SQAP outline, CoQ, risk register | Four items reclassified from quality requirements to constraints | Targets and effort estimates checked as realistic for a three-person team |
| 4 | 2026-10-04 | Claude Opus 5 | Repository setup and coding standard | Asked for a Checkstyle configuration enforcing C-01, a CODEOWNERS file, a PR template and branch rules | Checkstyle config, CODEOWNERS, PR template, branch rules | C-01 check rewritten as a token rule after a false positive | Branch protection tested by a direct push, which was blocked and logged |
| 5 | 2026-10-04 | Claude Opus 5 | Business Rules Register | Asked to turn each module's rules into numbered rules with boundary values for test design | 43 numbered rules with boundary values | Numeric thresholds reviewed and accepted unchanged by the team | Boundary values cross-checked against the RTM test design |
| 6 | 2026-10-04 | Claude Opus 5 | Build and CI scaffold | Asked for a Gradle build, a failing `MoneyTest` first, then `Money`, and a GitHub Actions workflow | Gradle build, `Money`, `MoneyTest`, GitHub Actions workflow | Written as a real TDD cycle, red commit before green | CI green: compile, 11 tests, Checkstyle, coverage |
| 7 | 2026-10-04 | Claude Opus 5 | Submission PDFs | Asked for a pandoc/LaTeX script producing both submission PDFs with a title page | Build script and title page | Fonts changed to carry mathematical symbols | Pages inspected; a fault dropping `≤` and `×` was found and fixed |
| 8 | 2026-10-07 | Claude Opus 5.5 | Pre-submission review of both documents and the repository | Asked to check both M1 documents against the joint guidelines and rubrics, list inconsistencies, then apply the fixes | Corrections to both documents, RTM, risk register, business rules, README and CODEOWNERS: M1 Team Lead, class views matched to the text and RTM, SOLID status labels, milestone references matched to the updated guidelines, an independent fact-check pass, PDFs rebuilt with an HTML/CSS pipeline (Paged.js) | M1 Team Lead confirmed by the team; an interim four-milestone revision was reverted to three milestones when the instructor issued the updated guidelines the same day; risk-grade assignment and active-loan counting defined at the team lead's request (BR-03, BR-09) | Each change checked against the guidelines PDF and the live repository (ruleset, CI runs, `Money.java`); PDFs rebuilt and every page inspected |

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
