# MicroLend - Fagan inspection process

How the team runs formal Fagan-style inspections from M2 onward. The SE431 M2 rubric
(Fagan inspection report, 20 marks) requires an inspection of real code or design, roles
and process evidenced, **at least one inspector from a different team** (paired by the
instructor), and a defect log with severity and CAPA tracking.

Each inspection has its own folder `INS-nn-<short-name>/` holding its plan, checklist,
completed forms and minutes. Defects found go into [`../defect-log.md`](../defect-log.md),
which is the single defect and CAPA register for the project.

| Inspection | Work product | Status |
|---|---|---|
| [INS-01](INS-01-repayment-path/plan.md) | Repayment and ledger money path (`ledger`, `persistence`) | Planned |

## Roles

| Role | Responsibility | Who |
|---|---|---|
| Moderator | Checks entry criteria, schedules, runs the meeting, keeps it on finding (not fixing) defects, decides on re-inspection, signs exit | A team member who is **not** the author |
| Author | Supplies the package, answers questions, does the rework. Does not read or moderate | Owner of the inspected module |
| Reader | Paraphrases the code line by line in the meeting so inspectors react to the reading, not to their own notes | A team member who is not the author |
| Recorder | Writes each defect into the log during the meeting with location, type and severity | A team member; may be the moderator for a three-person team |
| Inspectors | Prepare individually against the checklist and raise defects in the meeting | All participants, including **at least one inspector from another team** |

A three-person team cannot fill every role with different people. The rule we keep is
that the author never moderates, reads or records their own work, and the external
inspector is a full inspector, not an observer.

## The six stages

| Stage | What happens | Output |
|---|---|---|
| 1. Planning | Moderator checks entry criteria, fixes scope (files and line ranges), assigns roles, agrees the date with the external inspector | `plan.md` completed |
| 2. Overview | Author gives a 10-15 minute walk-through of purpose, business rules and design context (BR-18 to BR-26, M1 design sections 3.6 and 3.7). No defect hunting | Overview noted in minutes |
| 3. Preparation | Each inspector reads the package alone against `checklist.md` and logs suspected defects with time spent | One preparation log per inspector in `forms.md` |
| 4. Inspection meeting | Reader paraphrases; inspectors raise defects; recorder logs them; moderator classifies type and severity. Solutions are not discussed | Meeting minutes; defects entered in `../defect-log.md` |
| 5. Rework | Author fixes every Major and each Minor accepted for fixing, on a feature branch through a reviewed PR, with the commit referencing the defect ID | Fix commits, defect status `Fixed` |
| 6. Follow-up | Moderator verifies each fix (and the test that would have caught it), closes defects, decides whether re-inspection is needed, opens CAPA where a cause is systemic | Follow-up section signed, CAPA rows updated |

## Entry criteria

- The code compiles and `.\gradlew.bat build` passes on the commit being inspected (CI green).
- Checkstyle reports no violations, so the meeting is not spent on layout or C-01 issues a tool already catches.
- The package is frozen at a named commit hash; inspectors all read the same version.
- Size is within limits: at most about 250 non-comment lines of code per meeting.
- Business rules referenced by the code exist in `docs/business-rules.md`.
- Preparation material (plan, checklist, code listing or commit link) is sent at least 48 hours before the meeting.

## Exit criteria

- Every logged defect has a type, severity and status.
- All Major defects are fixed and verified by the moderator, or deferred with written justification and a CAPA entry.
- Metrics are recorded: preparation time per inspector, meeting time, LOC inspected, defects by type and severity, defects per KLOC.
- Re-inspection is required if more than 5 Major defects are found or rework touches more than 20% of the inspected lines.

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
| Standards | Breach of CLAUDE.md constraints or Checkstyle rules not caught by the tool, generic exceptions, missing validation |
| Documentation | Javadoc or comments that contradict the code or the business rules |

## From defect to CAPA

1. The recorder gives each defect the next ID `DEF-nnn` in [`../defect-log.md`](../defect-log.md) with source `Inspection INS-nn`.
2. Each Major defect, and any defect type that recurs (3 or more of one type in one inspection or across inspections), gets a CAPA row `CAPA-nnn`.
3. The CAPA row records the root cause, the corrective action (fix this instance) and the preventive action (stop it recurring, for example a new checklist item, Checkstyle rule, test or review rule).
4. A CAPA is closed only when its verification evidence (commit hash, test name, CI run) is recorded.
5. Open CAPA items are reviewed at each milestone and reported in the M2 and M3 SE431 documents; the risk register is updated when a CAPA reveals a new or changed risk.
