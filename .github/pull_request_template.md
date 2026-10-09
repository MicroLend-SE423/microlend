## What this changes

<!-- One or two sentences. Which module, and what behaviour changed. -->

## Traceability

- **Requirement ID(s):** <!-- FR-xx / QR-xx from qa/rtm.md, or n/a with a reason -->
- **Module:** <!-- origination | products | ledger | delinquency | reporting -->

## TDD evidence

<!-- The rubric assesses red-green-refactor from repository history, so name the commits. -->

- **Red commit (failing test):** <!-- hash + subject -->
- **Green commit (implementation):** <!-- hash + subject -->
- **Refactor commit(s):** <!-- hash + subject, or "none needed" -->

## Checklist

- [ ] Tests added or updated, and the suite passes locally
- [ ] `./gradlew checkstyleMain` passes — no `double`/`float` for money (constraint C-01)
- [ ] `BigDecimal` compared with `compareTo() == 0`, not `equals()`
- [ ] Public API has Javadoc; invariants are stated where they are enforced
- [ ] No new public mutable state, and no internal collection returned unprotected
- [ ] RTM updated if a requirement's status changed
- [ ] Risk register updated if this work raised, lowered or closed a risk
- [ ] AI Usage Log updated if an AI tool contributed to this change

## Review

<!-- The author does not merge their own PR. The reviewer merges it. -->

- **Reviewer:** <!-- a team member who does not own this module -->
- **Merge method:** merge commit (`--no-ff`). Squash and rebase are disabled, so the TDD trail survives.
