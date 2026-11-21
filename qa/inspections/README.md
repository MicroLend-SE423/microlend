# MicroLend - Fagan inspection process

Each inspection has a folder `INS-nn-<short-name>/` (plan, checklist, forms, minutes); its
defects go into [`../defect-log.md`](../defect-log.md), the single defect and CAPA register. The
SE431 M2 rubric (Fagan inspection report, 20 marks) requires **at least one inspector from a
different team**, paired by the instructor.

| Inspection | Work product | Status |
|---|---|---|
| [INS-01](INS-01-repayment-path/plan.md) | Repayment and ledger money path (`ledger`, `persistence`) | Planned, not held: on 21 Nov 2026 no inspector from another team has been assigned (risk R3). Package complete; scope recounted on the merged code (meeting A about 228 NCLOC) |

## Roles

| Role | Responsibility | Who |
|---|---|---|
| Moderator | Checks entry criteria, schedules, runs the meeting, keeps it on finding (not fixing) defects, decides on re-inspection, signs exit | A team member who is **not** the author |
| Author | Supplies the package, answers questions, does the rework. Does not read or moderate | Owner of the inspected module |
| Reader | Paraphrases the code line by line in the meeting | A team member who is not the author |
| Recorder | Writes each defect into the log during the meeting with location, type and severity | A team member; may be the moderator for a three-person team |
| Inspectors | Prepare individually against the checklist and raise defects in the meeting | All participants, including **at least one inspector from another team** |

With three members roles double up, but the author never moderates, reads or records their own
work, and the external inspector is a full inspector, not an observer.

## The six stages

| Stage | What happens | Output |
|---|---|---|
| 1. Planning | Moderator checks entry criteria, fixes scope (files and line ranges), assigns roles, agrees the date with the external inspector | `plan.md` completed |
| 2. Overview | Author's 10-15 minute walk-through of purpose, business rules and design (BR-18 to BR-26, M1 design sections 3.6 and 3.7); no defect hunting | Overview noted in minutes |
| 3. Preparation | Each inspector reads the package alone against `checklist.md` and logs suspected defects with time spent | One preparation log per inspector in `forms.md` |
| 4. Inspection meeting | Reader paraphrases; inspectors raise defects; recorder logs them; moderator classifies type and severity. Solutions are not discussed | Meeting minutes; defects entered in `../defect-log.md` |
| 5. Rework | Author fixes every Major and each accepted Minor through a reviewed PR from a feature branch; the commit cites the defect ID | Fix commits, defect status `Fixed` |
| 6. Follow-up | Moderator verifies each fix (and the test that would have caught it), closes defects, decides on re-inspection, opens CAPA where a cause is systemic | Follow-up section signed, CAPA rows updated |

## Entry criteria

- `.\gradlew.bat build` passes on the inspected commit (CI green).
- Checkstyle clean, so no meeting time goes on layout or C-01 issues.
- Package frozen at a named commit hash.
- At most about 250 non-comment lines of code (NCLOC) per meeting.
- Referenced business rules exist in `docs/business-rules.md`.
- Plan, checklist and code listing or commit link sent at least 48 hours ahead.

## Exit criteria

- Every logged defect has a type, severity and status.
- Every Major defect fixed and verified by the moderator, or deferred with written justification and a CAPA.
- Metrics recorded: preparation time per inspector, meeting time, LOC inspected, defects by type and severity, defects per KLOC.
- Re-inspection if more than 5 Major defects are found or rework touches more than 20% of the inspected lines.

## Rates and limits

| Measure | Limit | Reason |
|---|---|---|
| Preparation rate | about 100-125 LOC per hour | Faster reading finds markedly fewer defects |
| Meeting inspection rate | at most 150 LOC per hour | Keeps the reader's paraphrase meaningful |
| Meeting length | at most 2 hours | Attention falls off after 2 hours; split the package instead |
| Package size | at most about 250 NCLOC per meeting | Fits within the two limits above |

## Defect classification

**Severity**

| Severity | Meaning in MicroLend |
|---|---|
| Major | Would cause a wrong result, lost or unbalanced money, a broken invariant (BR-23, BR-24), data corruption, a security hole, or a failure visible to a user or the end-of-day run |
| Minor | Does not change behaviour now: unclear naming, missing Javadoc, weak test, maintainability or standards issue |

**Type**

| Type | Examples |
|---|---|
| Logic | Wrong allocation order, wrong comparison, off-by-one at a BVA boundary, missing branch |
| Data | Wrong scale or rounding, `double`/`float` use, paisa conversion errors, wrong column or constraint |
| Interface | Wrong contract between modules, null passed across a boundary, misuse of `LedgerWriter`/`LedgerReader`, transaction handled by the wrong caller |
| Standards | Breach of the C-01 to C-04 constraints or Checkstyle rules not caught by the tool, generic exceptions, missing validation |
| Documentation | Javadoc or comments that contradict the code or the business rules |

## From defect to CAPA

1. The recorder assigns the next `DEF-nnn` in [`../defect-log.md`](../defect-log.md), source `Inspection INS-nn`.
2. Each Major defect, and any type recurring 3 or more times (in one inspection or across inspections), gets a `CAPA-nnn` row.
3. The row gives root cause, corrective action (this instance) and preventive action (a new checklist item, Checkstyle rule, test or review rule).
4. A CAPA closes only when its verification evidence (commit hash, test name, CI run) is recorded.
5. Open CAPA are reviewed each milestone and reported in the SE431 M2 and M3 documents; a new or changed risk they reveal goes into the risk register.
