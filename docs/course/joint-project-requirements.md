# Joint Project Requirements and Rubrics (condensed)

Condensed by the team from the official *[Updated] SE423 × SE431 Joint Semester Project
Guidelines* (Fall 2026), for planning. The PDF on Teams is authoritative; check it when
in doubt.

## Minimum requirements

### SE423 — Construction
- At least 4 functional modules, each with real business logic.
- OO design showing SOLID, with a written justification of where and why each principle applies.
- At least 3 correctly implemented, justified design patterns: ≥ 1 creational and ≥ 1 structural or behavioural.
- At least one genuinely concurrent or asynchronous operation with a documented thread-safety strategy.
- Defensive programming and structured exception handling across module boundaries.
- A persistent data layer with a defined schema.
- An automated test suite built test-first (TDD), ≥ 60% of core business logic by M3, with a visible red-green-refactor commit history (do not squash-merge).
- A hosted Git repository with individually attributable history from every member.
- A CI pipeline that builds and runs the tests on push.

### SE431 — SQA
- A complete IEEE 730-structured SQAP, specific to this project.
- An RTM kept up to date at every milestone.
- At least one formal Fagan-style inspection with a defect log and CAPA tracking; at M2 it must include **at least one inspector from a different team** (the instructor pairs teams).
- A static analysis report from a real tool, findings triaged and resolved or justified.
- Dynamic test design: black-box (EP, BVA) and white-box (CFG, cyclomatic complexity, basis paths), executed with a coverage report.
- Documented SCM: branching strategy, merge/quality gates, baselines, with evidence from the repo.
- A GQM programme: ≥ 1 goal, 2–3 questions, metrics from real project data.
- A risk register started at M1 with ≥ 5 risks, updated every milestone; each with probability, impact, priority, mitigation, owner, status and residual risk.

## Schedule

| Week | SE423 | SE431 | Weight |
|---|---|---|---|
| 6 | M1 — proposal, OOD, pattern plan | M1 — quality requirements, RTM v1, SQAP scope | 15% |
| 10 | M2 — working architecture, patterns, concurrency, verification baseline | M2 — inspection, static analysis, V&V, test design, SCM baseline | 35% |
| 13 | M3 — final integrated system, TDD, refactoring, CI, demo | M3 — final SQAP, test execution, GQM, risk register, audit | 50% |

The project is 15% of SE423 and 10% of SE431. Bands per criterion: Full 100% · Good 80% · Adequate 60% · Poor 30% · Missing 0%.

## Rubrics

### SE423 M2 (/100)
| Criterion | Wt | Evidence |
|---|---|---|
| Working core architecture | 20 | Builds and runs end-to-end; module boundaries match M1 (deviations justified); substantial progress across modules |
| Design patterns implemented | 20 | ≥ 3 (≥ 1 creational, ≥ 1 structural/behavioural), each solving a real problem, integrated in running code; rationale, benefit, rejected alternative explainable |
| Defensive programming & exceptions | 10 | Validation at module/system boundaries; structured, non-generic exceptions with logging/error reporting |
| Concurrency & thread safety | 15 | A real concurrent/async operation; documented strategy consistent with the code |
| Code quality & SOLID | 15 | SOLID visible in code; no major unjustified violations; reasonable cohesion/coupling |
| TDD & test baseline | 10 | Tests run against real logic; genuine red-green-refactor history; coverage measured (60% not yet required) |
| Repository discipline | 10 | Continuous, individually attributable commits, branches, PRs |

### SE423 M3 (/100)
| Criterion | Wt | Evidence |
|---|---|---|
| Final completeness & correctness | 20 | All 4+ modules complete and integrated; all §3.1 minimums met; usable end-to-end |
| TDD suite & coverage | 15 | ≥ 60% of core logic by real tool output; relevant, effective tests; red-green-refactor history |
| Refactoring & code smells | 15 | Before/after evidence for ≥ 3 smells; behaviour preserved, tests still pass |
| Concurrency correctness | 10 | Concurrency tested; protection against races/deadlocks shown; ≥ 2 injected failure scenarios handled gracefully |
| CI pipeline | 10 | Builds and tests on push; evidence of multiple genuine runs |
| Architecture, SOLID & pattern integrity | 10 | Earlier decisions still integrated; deviations justified |
| Final documentation | 10 | Docs match the final code; README, setup and pattern rationale current |
| Live demo & individual Q&A | 10 | Demo from the submitted build; every member defends their own module |

### SE431 M2 (/100)
| Criterion | Wt | Evidence |
|---|---|---|
| Fagan inspection report | 20 | On real code/design; roles and process evidenced; ≥ 1 inspector from another team; defect log with severity and CAPA |
| Static analysis report | 15 | Real tool on the real codebase; findings triaged fixed / deferred / false positive; significant ones linked to corrective action |
| V&V plan (IEEE 1012) | 15 | V vs V distinguished; activities tied to real requirements and the remaining milestone; responsibilities, timing, evidence |
| Black-box test design | 15 | EP and BVA on real inputs; test cases traced to RTM IDs |
| White-box test design | 15 | CFG and cyclomatic complexity for key modules; basis-path test cases |
| SCM quality-gate evidence | 10 | Branching/merge strategy documented and followed; repo controls, reviews, baselines shown |
| RTM, risk & defect/CAPA update | 10 | RTM reflects the M2 build; risk register updated from real evidence; defects and CAPA current |

### SE431 M3 (/100)
| Criterion | Wt | Evidence |
|---|---|---|
| Final IEEE 730 SQAP | 25 | All mandatory sections, specific and consistent with the final system, repo, tests, risks and SCM; earlier artifacts integrated, not pasted |
| Test execution & coverage report | 20 | Designed black/white-box tests executed on the final build; real results and coverage; failures, defects, CAPA documented; traceable to requirement IDs |
| GQM programme | 15 | ≥ 1 goal, 2–3 questions, derived metrics, real data interpreted |
| Final risk register | 15 | Continuous updates M1→M3; ≥ 5 complete risks incl. residual; mitigation effectiveness judged from evidence |
| Final audit & CAPA closure | 10 | Earlier findings reviewed; CAPA closed or open with justification; final quality status consistent |
| Final RTM, SCM & defect traceability | 5 | RTM shows final verification status; repo consistent with documented gates; outstanding defects traceable |
| Presentation & individual Q&A | 10 | Each member explains and defends the quality activities they owned |

## Individual marks

Individual milestone mark = team score × 0.80 + individual evidence × 0.20. Individual
evidence comes from attributable commits, PRs, issues, tests and document history; the
quality of assigned work; participation in reviews and integration; the ability to
explain claimed work; and confidential peer evaluations (everyone must submit one,
including a self-evaluation). Commit count alone is not proof of contribution.

## Submission and AI use

- One submission per team per course on the Teams assignment: PDF document(s) plus the repository link. M2: be ready to demo the build on request. M3: final demo and individual Q&A.
- AI tools are allowed, but every member must be able to explain and defend what they claim. Keep an **AI Usage Log** for each milestone, recording for each significant use: the tool and model, the purpose, a prompt or activity summary, the material adopted, the modifications made and the verification performed.
