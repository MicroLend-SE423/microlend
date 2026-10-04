# MicroLend — AI Usage Log

**Required by the joint-project guidelines §9 (Academic Integrity & AI Use), submitted with every milestone.**

The guidelines permit AI-assisted development, but assessment is based on whether the team understands, verifies and can defend what it submits. Any member who cannot explain or justify work claimed as theirs may receive reduced individual marks regardless of the team's score.

**How this log is maintained:** one row per significant use, added **at the time of use**, not reconstructed before a deadline. The *Verification* column names what was actually checked.

---

## Milestone 1

| # | Date | Tool (name + model) | Purpose | Prompt / activity summary | AI-generated material adopted | Modifications made | Verification performed |
|---|---|---|---|---|---|---|---|
| 1 | 2026-10-04 | Claude Opus 5 (Claude Code) | Select a project that satisfies both course rubrics | Asked for candidate projects meeting every Section 3.1/3.2 minimum, simple to build, and defensible in a viva. Three candidates were analysed and scored | The comparative analysis in `project/Project-Idea-Analysis.md`; MicroLend selected over SkyHold (airline seats) and DispenseRx (pharmacy) | Scope reduced to a locally-run application with no deployment, no hosted services and no paid resources. DispenseRx rejected by the team on the stated ground that its clinical data would be invented and indefensible in questioning | Fit table checked row by row against the guidelines PDF; each claimed minimum requirement traced to where it is met. Team reviewed and accepted the recommendation on 4 Oct 2026 |
| 2 | 2026-10-04 | Claude Opus 5 (Claude Code) | Draft the SE423 M1 document | Asked for a document following the six M1 rubric criteria for the chosen project | Draft of `docs/M1-SE423-Proposal-OOD-Pattern-Plan.md`: class model, SOLID analysis, pattern justifications, cohesion/coupling reasoning | Single wide class diagram replaced by three per-area views after the first PDF proved illegible. "Command-line interface" replaced throughout with a locally served web interface, after the team confirmed the intended form of the application | Pattern choices checked against the taught material: an earlier draft's "Factory Method" was found to be a Simple Factory — which Lecture 11 states is *not* the GoF pattern — and was restructured so `LoanProduct` subclasses are the ConcreteCreators. Team reviewed and accepted the document on 4 Oct 2026 |
| 3 | 2026-10-04 | Claude Opus 5 (Claude Code) | Draft the SE431 M1 document | Asked for quality requirements, RTM, SQAP outline, Cost of Quality, standards awareness and risk register against the SE431 M1 rubric | Draft of `docs/M1-SE431-Quality-Requirements-RTM-SQAP.md` | Usability and portability entries rewritten for a locally-run web application. Quality requirements restated so each names one of the four standard verification methods; four items reclassified from quality requirements to constraints (C-01 to C-04) | Requirement targets and the Cost of Quality estimate reviewed for realism against the team's available time. Team reviewed and accepted the document on 4 Oct 2026 |
| 4 | 2026-10-04 | Claude Opus 5 (Claude Code) | Repository, quality gates and coding standard | Asked to create the repository with branch protection, CODEOWNERS, a pull-request template and a Checkstyle configuration enforcing the money constraint | `config/checkstyle.xml`, `.github/CODEOWNERS`, `.github/pull_request_template.md`, README branching section | The C-01 check was rewritten from a text pattern to a token-based rule (`IllegalToken` on `NUM_DOUBLE`/`NUM_FLOAT`) after CI showed the original flagging the version string `"0.1.0-M1"` as a decimal literal | Branch protection tested by attempting a direct push to `main`, which GitHub reported as a bypassed rule violation. The Checkstyle fault was found by the CI pipeline itself and corrected before the build went green |
| 5 | 2026-10-04 | Claude Opus 5 (Claude Code) | Business Rules Register | Asked to turn the described rules into a numbered register with boundary values for test design | `docs/business-rules.md` — 43 rules across the five modules | None to the rule set. The six numeric decisions listed in §8 (40% burden ceiling, 0.05%/day penalty capped at 10%, 180-day write-off, risk-grade spreads, provision percentages, 3-month grace cap) were reviewed and **accepted unchanged** by the team on 4 Oct 2026 | Boundary values cross-checked against the test design techniques recorded in the RTM, so every rule with a threshold has a corresponding boundary test planned |
| 6 | 2026-10-04 | Claude Opus 5 (Claude Code) | Build, test scaffold and CI pipeline | Asked for a Gradle build, the first domain class written test-first, and a CI pipeline, with nothing that incurs cost | `build.gradle`, Gradle wrapper, `Money`, `LoanDomainException`, `InvalidMoneyException`, `MoneyTest`, `.github/workflows/build.yml` | Written as a genuine TDD cycle: the failing test was committed first (`test(red):`), then the implementation (`feat(green):`). `Money.minus` was given a negative-result guard so the type cannot express direction — the ledger's debit and credit columns do that | CI run on the public repository: compile, 10 JUnit tests, Checkstyle gate and coverage report all pass. Two faults were found and fixed through CI: a missing executable bit on `gradlew`, and the Checkstyle false positive in row 4 |
| 7 | 2026-10-04 | Claude Opus 5 (Claude Code) | Submission PDFs | Asked for a PDF build with a GIKI title page carrying the team names, project title and course code | `docs/build-pdf.sh`, `docs/header.tex` | Fonts changed to Times New Roman with explicit Unicode mappings | Rendered pages inspected individually. A font fault was found that silently dropped `≤`, `≥`, `≈` and `×` from the PDFs — which would have corrupted the stated quality targets — and was corrected before submission |

### Declaration for Milestone 1

> I confirm that I have read, understood and verified the material submitted under my name, that I can explain and defend the design decisions, quality requirements and analysis it contains, and that any AI-assisted content was reviewed and modified by me where needed.

| Member | Reg. # | Sections owned | Signature / date |
|---|---|---|---|
| Muhammad Ibrahim | 2023446 | `origination`, `delinquency`; SQAP outline, risk register | |
| Hassan Khalid | 2023242 | `products`, `reporting`; quality requirements, RTM | |
| Tughral Hussain | 2023532 | `ledger`; standards awareness, Cost of Quality, repository and build setup | |

---

## Milestone 2

*(rows added as work proceeds)*

| # | Date | Tool | Purpose | Prompt / activity summary | Adopted | Modifications | Verification |
|---|---|---|---|---|---|---|---|

---

## Milestone 3

*(rows added as work proceeds)*

| # | Date | Tool | Purpose | Prompt / activity summary | Adopted | Modifications | Verification |
|---|---|---|---|---|---|---|---|

---

## Rules the team has agreed for AI use on this project

1. **Generated code enters the repository only through the module owner**, who reviews it line by line and rewrites what they would have written differently. The owner is the one answering for it in the viva.
2. **No generated test is accepted without reading what it asserts.** A test that passes for the wrong reason is worse than no test.
3. **Business rules and numeric thresholds are the team's decisions,** not suggestions accepted by default.
4. **Every row in this log is added when the work happens.** A log assembled the night before a deadline is itself evidence of the problem the requirement is designed to catch.
5. **If a member cannot explain a submitted artifact, it is withdrawn or rewritten before submission** rather than defended in the demo.
