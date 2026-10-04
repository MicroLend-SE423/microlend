# MicroLend — AI Usage Log

**Required by the joint-project guidelines §9 (Academic Integrity & AI Use), submitted with every milestone.**

The guidelines permit AI-assisted development, but assessment is based on whether the team understands, verifies and can defend what it submits. Any member who cannot explain or justify work claimed as theirs may receive reduced individual marks regardless of the team's score.

**How this log is maintained:** one row per significant use, added **at the time of use**, not reconstructed before a deadline. The *Verification* column must name what was actually checked, by whom. A row whose verification is still outstanding is marked **PENDING** and must be closed before the milestone is submitted.

---

## Milestone 1

| # | Date | Tool (name + model) | Purpose | Prompt / activity summary | AI-generated material adopted | Modifications made by the team | Verification performed | Member |
|---|---|---|---|---|---|---|---|---|
| 1 | 2026-10-04 | Claude Opus 5 (Claude Code) | Evaluate candidate project ideas against the guidelines | Asked for project options that satisfy every Section 3.1/3.2 minimum, are simple to build, and are defensible in a viva; three candidates were analysed against the rubric | The comparative analysis in `project/Project-Idea-Analysis.md`, and the MicroLend concept | *(to complete)* | Team reviewed the fit table against the guidelines PDF and confirmed each claimed requirement is genuinely met — **PENDING** | *(to assign)* |
| 2 | 2026-10-04 | Claude Opus 5 (Claude Code) | Draft the SE423 M1 document | Asked for a document following the six M1 rubric criteria for the chosen project | Draft structure and prose of `docs/M1-SE423-Proposal-OOD-Pattern-Plan.md`, including the class model, SOLID analysis and pattern justifications | *(to complete — each owner is to review and rewrite the sections describing their own modules)* | Each module owner must confirm the design described matches what they intend to build and can explain it — **PENDING** | all three |
| 3 | 2026-10-04 | Claude Opus 5 (Claude Code) | Draft the SE431 M1 document | Asked for quality requirements, RTM, SQAP outline, CoQ, standards awareness and risk register against the SE431 M1 rubric | Draft of `docs/M1-SE431-Quality-Requirements-RTM-SQAP.md` | *(to complete)* | Quality requirement targets and the CoQ estimates must be checked as realistic for this team before submission — **PENDING** | Hassan Khalid (QRs/RTM), Muhammad Ibrahim (SQAP/risks), Tughral Hussain (standards/CoQ) |
| 4 | 2026-10-04 | Claude Opus 5 (Claude Code) | Repository scaffold and quality gates | Asked to create the repository, branch-protection ruleset, CODEOWNERS, PR template and Checkstyle configuration | `config/checkstyle.xml`, `.github/CODEOWNERS`, `.github/pull_request_template.md`, README branching section | *(to complete)* | Branch protection was tested by attempting a direct push to `main`, which was reported as a bypassed rule violation — **VERIFIED** | Tughral Hussain |
| 5 | 2026-10-04 | Claude Opus 5 (Claude Code) | Business Rules Register | Asked to turn the design's described rules into a numbered register with boundary values | `docs/business-rules.md` | *(to complete — §8 lists the numeric decisions the team must confirm or change)* | The six open decisions in §8 must be explicitly accepted or changed by the team — **PENDING** | all three |
| 6 | 2026-10-04 | Claude Opus 5 (Claude Code) | PDF build tooling | Asked for a script that renders the Mermaid diagrams and builds submission PDFs with a title page | `docs/build-pdf.sh`, `docs/header.tex` | *(to complete)* | Both PDFs were rendered and inspected page by page; a font fault that silently dropped `≤`, `≥` and `×` was found and corrected — **VERIFIED** | Tughral Hussain |

### Declaration for Milestone 1

*To be completed and signed by each member before submission:*

> I confirm that I have read, understood and verified the material submitted under my name, that I can explain and defend the design decisions, quality requirements and analysis it contains, and that any AI-assisted content was reviewed and modified by me where needed.

| Member | Reg. # | Sections owned | Confirmed |
|---|---|---|---|
| Muhammad Ibrahim | 2023446 | `origination`, `delinquency`; SQAP outline, risk register | ☐ |
| Hassan Khalid | 2023242 | `products`, `reporting`; quality requirements, RTM | ☐ |
| Tughral Hussain | 2023532 | `ledger`; standards awareness, Cost of Quality, repository setup | ☐ |

---

## Milestone 2

*(rows added as work proceeds)*

| # | Date | Tool | Purpose | Prompt / activity summary | Adopted | Modifications | Verification | Member |
|---|---|---|---|---|---|---|---|---|

---

## Milestone 3

*(rows added as work proceeds)*

| # | Date | Tool | Purpose | Prompt / activity summary | Adopted | Modifications | Verification | Member |
|---|---|---|---|---|---|---|---|---|

---

## Rules the team has agreed for AI use on this project

1. **Generated code enters the repository only through the module owner**, who reviews it line by line and rewrites what they would have written differently. The owner is the one answering for it in the viva.
2. **No generated test is accepted without reading what it asserts.** A test that passes for the wrong reason is worse than no test.
3. **Business rules and numeric thresholds are the team's decisions,** not suggestions accepted by default. The open decisions in `docs/business-rules.md` §8 exist for exactly this reason.
4. **Every row in this log is added when the work happens.** A log assembled the night before a deadline is itself evidence of the problem the requirement is designed to catch.
5. **If a member cannot explain a submitted artifact, it is withdrawn or rewritten before submission** rather than defended in the demo.
