# MicroLend — AI Usage Log

**Required by the joint-project guidelines §9 (Academic Integrity & AI Use), submitted with every milestone.**

The guidelines permit AI-assisted development, but assessment is based on whether the team understands, verifies and can defend what it submits. Any member who cannot explain or justify work claimed as theirs may receive reduced individual marks regardless of the team's score.

**How this log is maintained:** one row per significant use, added **at the time of use**, not reconstructed before a deadline. The *Verification* column names what was actually checked.

---

## Milestone 1

### Entry 1 — Select a project that satisfies both course rubrics

**Date:** 4 October 2026 · **Tool:** Claude Opus 5 (Claude Code)

**Activity.** Asked for candidate projects meeting every Section 3.1 and 3.2 minimum, simple to build, and defensible in a viva. Three candidates were analysed and scored against the rubric.

**Adopted.** The comparative analysis in `project/Project-Idea-Analysis.md`; MicroLend selected over SkyHold (airline seat inventory) and DispenseRx (pharmacy dispensing).

**Modifications made.** Scope reduced to an application that runs locally with no deployment, no hosted services and no paid resources. DispenseRx rejected by the team on the stated ground that its clinical data would be invented and therefore indefensible under questioning.

**Verification performed.** Fit table checked row by row against the guidelines PDF, with each claimed minimum traced to where it is met. Team reviewed and accepted the recommendation on 4 October 2026.

### Entry 2 — Draft the SE423 M1 document

**Date:** 4 October 2026 · **Tool:** Claude Opus 5 (Claude Code)

**Activity.** Asked for a document following the six M1 rubric criteria for the chosen project.

**Adopted.** Draft of `docs/M1-SE423-Proposal-OOD-Pattern-Plan.md`: class model, SOLID analysis, pattern justifications and the cohesion/coupling reasoning.

**Modifications made.** The single wide class diagram was replaced by three per-area views after the first PDF proved illegible. "Command-line interface" was replaced throughout with a locally served web interface, once the team confirmed the intended form of the application.

**Verification performed.** Pattern choices checked against the taught material. An earlier draft’s "Factory Method" was found to be a Simple Factory — which Lecture 11 states is *not* the GoF pattern — and was restructured so that `LoanProduct` subclasses are the ConcreteCreators. Team reviewed and accepted the document on 4 October 2026.

### Entry 3 — Draft the SE431 M1 document

**Date:** 4 October 2026 · **Tool:** Claude Opus 5 (Claude Code)

**Activity.** Asked for quality requirements, an RTM, the SQAP outline, Cost of Quality, standards awareness and a risk register, against the SE431 M1 rubric.

**Adopted.** Draft of `docs/M1-SE431-Quality-Requirements-RTM-SQAP.md`.

**Modifications made.** Usability and portability entries rewritten for a locally-run web application. Quality requirements restated so each names one of the four standard verification methods, and four items reclassified from quality requirements to constraints (C-01 to C-04).

**Verification performed.** Requirement targets and the Cost of Quality estimate reviewed for realism against the team’s available time. Team reviewed and accepted the document on 4 October 2026.

### Entry 4 — Repository, quality gates and coding standard

**Date:** 4 October 2026 · **Tool:** Claude Opus 5 (Claude Code)

**Activity.** Asked to create the repository with branch protection, CODEOWNERS, a pull-request template, and a Checkstyle configuration enforcing the money constraint.

**Adopted.** `config/checkstyle.xml`, `.github/CODEOWNERS`, `.github/pull_request_template.md`, and the README branching section.

**Modifications made.** The C-01 check was rewritten from a text pattern to a token-based rule (`IllegalToken` on `NUM_DOUBLE` and `NUM_FLOAT`) after CI showed the original flagging the version string `"0.1.0-M1"` as a decimal literal.

**Verification performed.** Branch protection tested by attempting a direct push to `main`, which GitHub reported as a bypassed rule violation. The Checkstyle fault was found by the CI pipeline itself and corrected before the build went green.

### Entry 5 — Business Rules Register

**Date:** 4 October 2026 · **Tool:** Claude Opus 5 (Claude Code)

**Activity.** Asked to turn the described rules into a numbered register with boundary values for test design.

**Adopted.** `docs/business-rules.md` — 43 rules across the five modules.

**Modifications made.** None to the rule set. The six numeric decisions listed in §8 — the 40% burden ceiling, the 0.05%/day penalty capped at 10%, the 180-day write-off, the risk-grade spreads, the provision percentages and the 3-month grace cap — were reviewed and **accepted unchanged** by the team on 4 October 2026.

**Verification performed.** Boundary values cross-checked against the test design techniques recorded in the RTM, so every rule carrying a threshold has a corresponding boundary test planned.

### Entry 6 — Build, test scaffold and CI pipeline

**Date:** 4 October 2026 · **Tool:** Claude Opus 5 (Claude Code)

**Activity.** Asked for a Gradle build, the first domain class written test-first, and a CI pipeline, with nothing that incurs cost.

**Adopted.** `build.gradle`, the Gradle wrapper, `Money`, `LoanDomainException`, `InvalidMoneyException`, `MoneyTest`, and `.github/workflows/build.yml`.

**Modifications made.** Written as a genuine TDD cycle: the failing test was committed first (`test(red):`), then the implementation (`feat(green):`). `Money.minus` was given a negative-result guard so the type cannot express direction — the ledger’s debit and credit columns do that.

**Verification performed.** CI run on the public repository: compile, 10 JUnit tests, the Checkstyle gate and the coverage report all pass. Two faults were found and fixed through CI — a missing executable bit on `gradlew`, and the Checkstyle false positive described in entry 4.

### Entry 7 — Submission PDFs

**Date:** 4 October 2026 · **Tool:** Claude Opus 5 (Claude Code)

**Activity.** Asked for a PDF build with a GIKI title page carrying the team names, project title and course code.

**Adopted.** `docs/build-pdf.sh` and `docs/header.tex`.

**Modifications made.** Fonts changed to Times New Roman with explicit Unicode mappings.

**Verification performed.** Rendered pages inspected individually. A font fault was found that silently dropped the symbols for "less than or equal to", "greater than or equal to", "approximately" and "multiplied by" from the PDFs — which would have corrupted the stated quality targets — and was corrected before submission.

### Declaration for Milestone 1

> I confirm that I have read, understood and verified the material submitted under my name, that I can explain and defend the design decisions, quality requirements and analysis it contains, and that any AI-assisted content was reviewed and modified by me where needed.

**Muhammad Ibrahim — 2023446**
Sections owned: `origination`, `delinquency`; SQAP outline, risk register

> Signature: ............................................................  Date: ..............................

**Hassan Khalid — 2023242**
Sections owned: `products`, `reporting`; quality requirements, RTM

> Signature: ............................................................  Date: ..............................

**Tughral Hussain — 2023532**
Sections owned: `ledger`; standards awareness, Cost of Quality, repository and build setup

> Signature: ............................................................  Date: ..............................

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
