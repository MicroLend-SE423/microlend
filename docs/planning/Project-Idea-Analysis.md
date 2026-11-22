# SE423 × SE431 Joint Semester Project — Idea Analysis

**Team:** 3 students (referred to below as **S1**, **S2**, **S3**) · **Term:** Fall 2026
**Status at time of writing:** Milestone 1 (Week 6, 15% of the project grade) not yet submitted — the idea must be specifiable *immediately*, with no research spike needed before the proposal can be written.
**Brief from the team:** *"Something that looks complex but is very simple, where all SQA and SCD principles fit the rubrics naturally, and which we can easily defend and explain."*

**Sources used**
- `[Updated] SE423_SE431_Joint_Project_Guidelines.pdf` — all 9 pages (Sections 1–9, including both rubric sets and the individual-marking formula).
- SCD lectures Weeks 1–3 (Lec 01–09): encapsulation & invariant defence (the *Order checkout* example), cohesion & SRP, coupling & DIP with the **Adapter** example (`StripePaymentAdapter`), OCP via **Strategy** (`DiscountStrategy`), LSP, ISP+DIP.
- **SCD lectures Weeks 4–5 (Lec 10–13), slides + speaker notes:** the `ReportManager`/`CSVExporter` guided-inquiry activity; design patterns through the construction lens; the creational landscape (Factory Method, Abstract Factory, Builder, Prototype, Singleton) with the **Simple Factory vs Factory Method** distinction; structural patterns — **Adapter** (Client/Target/Adaptee/Adapter, Object Adapter by composition, `LegacyGatewayAdapter` dollars→cents) and **Facade** (`CheckoutFacade`); **Decorator** (`Notifier`/`EmailNotifier`/`NotifierDecorator`/`LoggingNotifier`, the subclass-explosion argument) and a first look at **Proxy**.
- SQA lectures Weeks 1–3 (Lec 01–07): error/defect/failure, Cost of Quality, ISO/IEC 25010 (SQuaRE), ISO/IEC/IEEE 29148 requirement characteristics, bidirectional traceability & the RTM, IEEE 730 §5.4.
- **SQA lectures Weeks 4–5 (Lec 08–09), slides + speaker notes:** software has no natural laws so process standards are our surrogate laws; the 1970s DoD "Big Bang" acquisition failure → DoD-STD-1679A → ISO/IEC/IEEE 12207; what a standard is (ISO 24765) and the four ISO development principles; standards as **prevention costs** in the PAF/CoQ model; 12207 process groups and IEEE 730's place inside them; safety-critical standards (DO-178C levels A–E, EN 50128 technique classification, ISO 13485/SaMD); ISO 9001:2015 (PDCA, process approach, risk-based thinking, the 7 QMPs), ISO/IEC 90003 as the software translation, CMMI-DEV's five maturity levels and Level-2/3 process areas, and **ISO/IEC 29110 for Very Small Entities** with the BitPerfect case study.

- **Course textbook: Laporte, C. Y. & April, A., *Software Quality Assurance* (Wiley/IEEE, 2018)** — the book the lectures are taught from. Sections drawn on here: **§3.3** (functional vs non-functional vs constraints; the seven characteristics of a good requirement; the four verification methods), **§3.2.3** (ISO 25000/25010), **§4.2** (standards as prevention costs; business models), **§4.3** (ISO 9000/9001, ISO/IEC 90003), **§4.4** (ISO/IEC/IEEE 12207), **§4.6 and Figure 4.7** (IEEE 730-2014 structure, the three SQA activity groups, and the authoritative SQAP table of contents), **§4.7.1 and Table 4.3** (CMMI-DEV maturity levels and process areas), **§4.7.6 and Tables 4.4/4.8** (ISO/IEC 29110, VSE definition, the four generic profiles, Deployment Packages), **§4.8** (DO-178C / EN 50128 / ISO 13485), **§4.9** (standards and the SQAP), **§5.5 and Figure 5.12** (the Fagan inspection process and IEEE 1028 inspection rates), **§11.3.1 and Table 11.7** (risk exposure = probability × consequence), and **Ch. 13 §13.2** (the SQAP section-by-section plan).

> **Validated against Lectures 10–13 (SCD), 08–09 (SQA) and the Laporte & April textbook, re-check dated 4 Oct 2026.**
> The new decks changed three things in this analysis: (a) the three patterns for the recommended project were **re-picked** so that every one of them is now formally taught — and one of the original three turned out to be a *Simple Factory*, which Lecture 11 explicitly says is **not** the GoF Factory Method; (b) **Decorator earned a place** and **Facade was considered and demoted to an optional fourth**; (c) a project-specific **standards-awareness** section was added for the SE431 M1 criterion, drawing on Lec 08–09. The shortlist order is unchanged, but the margin over Candidate 2 has narrowed — see "Does the ranking still stand?" at the end.

> Note on lecture alignment: SQA Week 03 Lecture 3 uses, as its worked SQAP example, *"ISO/IEC 25010 for Performance Efficiency, and an internal SQA policy requiring 100% Decision/Branch coverage for financial transactions."* Lecture 08's opening notes name *"a banking ledger leaks financial balances"* alongside a miscalculating medical device and a crashing train signalling module as the cases where "the AI wrote that part" is not a defence. A money-handling domain therefore lets the team quote the instructor's own examples back at them twice. This is a real, cheap advantage and it drives the recommendation below.

---

## 0. What "fits the rubric" actually means here

Pulled verbatim from the guidelines so every claim below is checkable:

**Section 3.1 — Construction (SE423) minimums**
1. ≥ 4 distinct functional modules, each with real business logic (not a single-file CRUD demo).
2. OO design demonstrating SOLID, with written justification of where and why each principle was applied.
3. ≥ 3 correctly implemented and justified patterns, **at least one creational** and **at least one structural or behavioural**; must be justifiable in the demo.
4. ≥ 1 genuinely concurrent or asynchronous operation with a documented thread-safety strategy.
5. Defensive programming and structured exception handling across module boundaries.
6. A persistent data layer with a defined schema.
7. Test-first (TDD) suite covering ≥ 60% of core business logic with visible red-green-refactor history (no squash-merges that flatten the trail).
8. Hosted Git repo with individually attributable contribution history.
9. CI that builds and runs the test suite on push.

**Section 3.2 — SQA (SE431) minimums**
10. Complete IEEE 730-structured SQAP, project-specific.
11. RTM kept current at every milestone.
12. ≥ 1 formal Fagan-style inspection with defect log and CAPA; **M2's inspection must include an inspector from a different team**.
13. Static-analysis report from a real tool, findings triaged.
14. Dynamic test design: black-box (EP, BVA) **and** white-box (CFG, cyclomatic complexity, basis path), executed, with a coverage report.
15. Documented SCM practices: branching strategy, merge/quality gates, baseline management, evidenced from the actual repo.
16. GQM programme: ≥ 1 Goal, 2–3 Questions, metrics collected from real project data.
17. Risk register initiated at M1 with ≥ 5 risks, each with probability, impact, priority, mitigation, owner, status, residual risk; updated at every milestone.

Two rubric lines that quietly decide the grade and that the shortlist is optimised for:
- **M1 / SE423, "Problem definition & requirement-to-minimum mapping" (15%)** rewards an *explicit mapping of each Section 3.1 minimum to how it will be met*. So the fit tables below are not analysis for its own sake — they are, almost verbatim, the deliverable.
- **M1 / SE423, "SOLID application plan" (20%)** requires analysing **all five** principles and saying where each is relevant, not relevant, or not yet observable; *claims of non-applicability must be justified*. A domain in which LSP and ISP are hard to even exemplify costs real marks.

---

# Candidate 1 (RECOMMENDED) — **MicroLend**: Community Microfinance Loan Servicing & Delinquency Engine

### One-line pitch
A back-office loan servicing system that originates small loans against a product catalogue, generates amortisation schedules, posts repayments through a strict allocation waterfall into an immutable double-entry ledger, and runs a concurrent end-of-day engine that accrues penalties, ages accounts into delinquency buckets and provisions the portfolio.

### Why it sounds impressive to a grader
"Loan servicing with an end-of-day batch engine, an append-only double-entry ledger and a delinquency classification model" is the vocabulary of core banking. It reads as a system with regulatory weight. In reality every rule is a table lookup, a date comparison or two lines of `BigDecimal` arithmetic; there is no UI beyond a CLI, no network, no third-party service, no algorithmic difficulty anywhere.

### Why it is actually very simple
- The entire system is **arithmetic over money and dates**, stored in SQLite. No framework beyond JUnit and a JDBC driver.
- Every business rule is a one-sentence statement the team invents and freezes at M1 in a *Business Rules Register* — nothing has to be researched or discovered.
- There is no "hard part". The riskiest code is the repayment allocation waterfall, which is ~40 lines, and that is precisely the code the SQA side needs for CFG/basis-path work.

### The modules (5 — one more than the minimum, which protects against scope cuts)

| # | Module | Core business rules (frozen at M1) | Primary owner |
|---|---|---|---|
| M1 | **Origination & Eligibility** | Applicant 18–65 at maturity; ≤ 2 active loans; declared monthly instalment burden ≤ 40% of declared income; no prior write-off; requested amount within the product's min/max; tenure within the product's band. Failing any rule yields a *typed* rejection reason, not a boolean. Risk grade A/B/C shifts the rate by a fixed spread and caps the approved amount. | **S1** |
| M2 | **Product Catalogue & Schedule Generation** | Three products: *flat-rate*, *reducing-balance (EMI)*, *bullet/single-payment*. Instalment dates follow a stated calendar rule (same day-of-month, clamped for short months); optional grace period defers the first instalment but still accrues interest; all rounding residue is absorbed into the final instalment so Σ instalments = principal + total interest exactly. | **S2** |
| M3 | **Repayment Posting & Ledger** | Allocation waterfall, in strict order: **penalty → fees → interest → principal**, oldest unpaid instalment first. Partial payments allowed; overpayment becomes an advance credit unless it clears the loan, in which case early-settlement rebate applies (unearned interest refunded for reducing-balance, no rebate for flat). Every posting writes balanced debit/credit ledger lines; a posted entry is **immutable** — a mistake is corrected only by a compensating reversal entry. | **S3** |
| M4 | **Delinquency, Penalty & Provisioning (EOD engine)** | Runs per business date over all active loans: accrues 0.05%/day penalty on overdue principal, capped at 10% of the overdue amount; computes days-past-due; buckets 0 / 1–30 / 31–60 / 61–90 / 90+ into Standard / Watch / Substandard / Doubtful / Loss; applies the matching provision percentage; auto-writes-off at DPD > 180. **This is the concurrent module.** | **S1** (impl.) + **S3** (ledger integration) |
| M5 | **Statements, Reporting & Alerts** | Customer statement for a date range; portfolio reports (PAR30, provision coverage, collection efficiency); CSV export; alert events raised on every classification change or bucket downgrade. | **S2** |

**Why this split works for the 20% individual-evidence component:** each owner holds a module whose invariants they can state in one sentence — S1: *"no loan may exist that the eligibility rules would not have approved"*; S2: *"the sum of a schedule's instalments equals principal plus total interest, exactly"*; S3: *"the ledger always balances and is never mutated"*. Those three sentences are also the three best answers available in an individual Q&A.

**SE431 deliverable ownership (rotating, as the guidelines' good example requires):**

| Milestone | SQAP & risk register | RTM & black-box design | Inspection, static analysis & white-box |
|---|---|---|---|
| M1 | S1 (lead) | S2 (lead) | S3 (lead) |
| M2 | S2 | S3 | S1 |
| M3 | S3 | S1 | S2 |

Everyone reviews everything; the lead signs the artefact. By M3 each member has led at least one artefact of each type — exactly the arrangement the guidelines call a "good example".

### Requirement-by-requirement fit table

| # | Requirement (guidelines) | Where it lives in MicroLend | Viva-ready justification (one sentence) | Stretch? |
|---|---|---|---|---|
| 1 | ≥ 4 modules, real business logic | The five modules above | "Each module owns a distinct decision — who qualifies, what the schedule is, how money is applied, how arrears age, what the portfolio looks like — and none is a CRUD screen." | No |
| 2a | **SRP** | `EligibilityEvaluator`, `ScheduleGenerator`, `RepaymentAllocator`, `DelinquencyClassifier`, `StatementRenderer` are separate; `LoanAccount` holds state and invariants only | "A change to the penalty rate touches only the delinquency module; a change to the statement layout touches only the renderer — different actors, different reasons to change." | No |
| 2b | **OCP** | The `LoanProduct` creator hierarchy with its `InterestCalculator` products, and `ProvisioningPolicy` | "Adding a 4th loan product means writing a new `LoanProduct` subclass and its calculator; no existing, tested class is edited — the same goal as the Lec 10 activity, where `ReportManager` had to stay untouched when `JsonExporter` arrived." | No |
| 2c | **LSP** | All `InterestCalculator` implementations honour the same contract: same preconditions, totals never negative, schedule length always = tenure | "We deliberately did *not* model an interest-free hardship loan as a subclass that throws on `accrue()` — that would weaken a postcondition and force `instanceof` at the call site; it is a calculator returning zero instead." | No |
| 2d | **ISP** | `RepaymentPoster`, `LedgerReader`, `ScheduleProvider`, `ReportExporter` are separate role interfaces; the EOD engine depends only on `ScheduleProvider` + `LedgerWriter` | "The EOD engine has no business being able to export CSV, so it is not handed an interface that can." | No |
| 2e | **DIP** | `LoanServicingService` depends on `LoanRepository` / `LedgerRepository` abstractions; the SQLite implementations are constructor-injected; tests inject in-memory fakes | "Our business policy never imports JDBC — the Week 2 Lecture 3 inversion, applied to persistence instead of Stripe." | No |
| 3 | ≥ 3 patterns, ≥ 1 creational + ≥ 1 structural/behavioural | **Factory Method** (creational) + **Adapter** (structural) + **Decorator** (structural); optional **Facade**, then **Strategy / State / Observer** after Week 6 | "All three are patterns we have been formally taught, each solves a problem that existed in our design before the pattern was named, and for each we can name the alternative we rejected." Satisfies the minimum with one creational and two structural. | No |
| 4 | ≥ 1 genuine concurrent/async op + thread-safety strategy | Module M4's EOD engine, running against live teller postings | See the concurrency section — the race is a classic lost update on a money balance. | No |
| 5 | Defensive programming + structured exception handling | Validation at the module boundary (`LoanApplication` constructor, `RepaymentCommand` factory); a typed exception hierarchy `LoanDomainException` → `IneligibleApplicantException`, `InvalidRepaymentException`, `LedgerImbalanceException`, `LoanClosedException`; `Money` rejects negative/over-scale values at construction | "No invalid object can exist: invariants are checked in the constructor, so the rest of the system never defends against states it cannot be in — the 'autonomous guardian of its own invariants' idea from Week 2." | No |
| 6 | Persistent layer with defined schema | SQLite: `customer`, `loan_product`, `loan`, `instalment`, `ledger_entry`, `payment`, `delinquency_snapshot` (7 tables, documented DDL, FKs, a `CHECK` enforcing non-negative amounts) | "The schema encodes the invariants the code enforces, so a corrupted write fails at two levels." | No |
| 7 | TDD ≥ 60% of core logic, visible red-green-refactor | Allocation waterfall, schedule generation, eligibility, penalty/bucketing are all pure functions — ideal test-first targets; JaCoCo report | "Commit convention `test(red):` → `feat(green):` → `refactor:`; merges are `--no-ff`, never squashed, so the trail survives." | No — but see risk R4 |
| 8 | Hosted Git, attributable history | GitHub; module ownership maps 1:1 to directories, so `git log --author` is meaningful; PRs reviewed by a non-owner | "Every file has an owner and every PR has a different reviewer — contribution and review are both attributable." | No |
| 9 | CI builds + runs tests on push | GitHub Actions: Maven/Gradle build, JUnit, JaCoCo threshold gate, SpotBugs/Checkstyle | "Free, no secrets, no network dependency, runs in under two minutes." | No |
| 10 | IEEE 730 SQAP | Written against this system; §5.4 tasks instantiated with our actual reviews, inspection, static analysis and test design | "Our SQAP names our classes and our thresholds, not generic text." | No |
| 11 | RTM at every milestone | Requirement IDs `FR-xx` / `QR-xx` → design element → class → test case ID → result | "Bidirectional: every requirement has a test, every test names a requirement — the two RTM smells from Week 3 Lecture 3 are what we audit for." | No |
| 12 | Fagan inspection + defect log + CAPA (external inspector at M2) | Inspect `RepaymentAllocator` + `DelinquencyClassifier` — the two highest-complexity, highest-consequence classes | "We inspected the money path, because that is where a defect costs the most — the CoQ argument from Week 2." | **Logistics only**: the external inspector must be arranged early — see risk R6 |
| 13 | Static analysis, triaged | SonarQube Community / SpotBugs + Checkstyle in CI; findings triaged fixed / deferred / justified-false-positive | "Our top finding category was `BigDecimal` equality and nullable returns — both real in a money domain." | No |
| 14 | EP + BVA + CFG + cyclomatic complexity + basis path, with coverage | **EP/BVA**: income ratio at 39.99 / 40.00 / 40.01%, age at 17/18/65/66, payment of 0 / 0.01 / exact-due / due+0.01, DPD at 0/1/30/31/90/91/180/181. **CFG/CC/basis path**: `RepaymentAllocator.post()` (CC ≈ 11) and `DelinquencyClassifier.classify()` (CC ≈ 8) | "Two methods with double-digit cyclomatic complexity sitting on the money path — the basis-path set is genuinely worth deriving, not a toy." | No — this is the idea's strongest single feature |
| 15 | SCM practices with evidence | `main` protected; `develop`; `feature/<module>-<id>`; PR + 1 non-owner approval + green CI as the merge gate; tags `m1-baseline`, `m2-baseline`, `m3-final` | "Baselines are tags, gates are branch protection rules — both are screenshot-able evidence from the real repo." | No |
| 16 | GQM with real data | **Goal:** reduce defect escape from the money path. **Q1:** are our highest-complexity methods our most defect-dense? **Q2:** is inspection cheaper per defect than testing? **Q3:** is coverage rising with code size? **Metrics:** defects/KLOC per module, defect-removal cost by phase, CC per method, branch coverage per milestone | "The CoQ model from Week 2 gives us the prevention/appraisal/failure cost buckets to fill with our own time sheets." | No |
| 17 | Risk register ≥ 5, maintained M1→M3 | Six starter risks below, re-scored at each milestone with residual risk | "Two of our risks are about the project, four about the product's quality — and R2 actually fired, which is why we added golden-schedule tests." | No |

**Nothing in this table is a stretch except the M2 external-inspector logistics, which is a scheduling task, not a technical one.**

### The three patterns — **re-picked against Lectures 10–13**

All three are now patterns the lecturer has taught in depth, with his own vocabulary and his own worked examples available as anchors. The selection follows his rule from Lecture 11 slide 14: *"Start from the construction problem, not from the pattern name."*

> **What changed and why.** The earlier draft named a `ScheduleGeneratorFactory` that selects a generator from a `product_type` string. Lecture 11 (slide 5 and its notes) is explicit that this is a **Simple Factory**, *not* the GoF Factory Method: *"A method such as `UserFactory.createUser()` certainly performs object creation, but that alone does not make it the GoF Factory Method pattern… Simple Factory: 'Give me some information, and I will decide what to create.' Factory Method: 'The particular creator subtype determines what gets created.'"* Claiming that as a Factory Method in the demo is a direct, avoidable loss of marks on the M2 "patterns correctly implemented" criterion (20%). It is restructured below into the real pattern. **Strategy** has been moved to the optional list and **Decorator** promoted in its place, because Decorator is now formally taught with a full code walkthrough while behavioural patterns as a family are not scheduled until Week 6.

**1. Factory Method — `LoanProduct` as Creator (creational)**
- *The construction problem:* `generateSchedule()` is a stable workflow — validate the tenure, build the due-date series, compute each instalment's interest, absorb the rounding residue into the final instalment, assemble the schedule. Only **one** step varies: which interest calculator is used. If the workflow writes `new ReducingBalanceCalculator()`, the schedule module is coupled to concrete classes through the `new` keyword (the DIP violation from the Lec 10 activity) and must be reopened for every product (the OCP violation).
- *The structure, in the lecturer's roles:* `LoanProduct` is the **Creator** and holds the stable `generateSchedule()` workflow; `createInterestCalculator()` is the **Factory Method**, declared by the Creator and overridden by `FlatRateProduct`, `ReducingBalanceProduct` and `BulletProduct` (the **ConcreteCreators**); `InterestCalculator` is the **Product** abstraction and the three calculators are the **ConcreteProducts**. This is the `ReportExporter.export()` → `createWriter()` shape from Lecture 11 slide 6, one-for-one.
- *Rejected alternative (also covered in the lectures):* **Simple Factory** — a `ScheduleFactory.create(type)` holding a `switch`. Rejected because the creation decision stays centralised in one place that must be edited for every new product; it moves the conditional ladder rather than removing it, and the Lec 10 deliverable was explicitly *"zero `new` keywords and no conditional checks"*. Also rejected: **Abstract Factory** — there is no product *family* here. Nothing in MicroLend resembles the on-premises/cloud incompatibility trap from Lec 10 Phase 3; adding a second factory layer to vary one product along one axis is indirection without a second dimension. Also rejected: **Singleton** for a shared product registry — Lecture 11 slide 12 warns this hides dependencies, creates global mutable state and leaks state between tests; constructor injection (DIP) already gives us one shared instance without the coupling.

**2. Adapter — `CsvStatementAdapter implements RepaymentSource` (structural)**
- *The construction problem:* branch repayments arrive as bank statement files whose column order, date format, and amount convention differ per bank — several of them report amounts in **minor units** (paisa/cents) as integers. Neither contract is wrong; they were simply designed independently. The servicing core must keep thinking in `Money` and `LocalDate`.
- *The structure, in the lecturer's four names:* the **Client** is the repayment-posting workflow; the **Target** is `RepaymentSource`, the interface our application wants; the **Adaptee** is each bank's raw statement reader; the **Adapter** is `HblStatementAdapter` / `McbStatementAdapter`, which converts `254900` → `Money(2549.00)` and `04102026` → `LocalDate`, and translates the reader's parse errors into our `InvalidRepaymentException`. It is an **Object Adapter**: it *has-a* reader rather than inheriting one (Lec 12 slide 8).
- *Rejected alternative (also covered in the lectures):* **Facade** — Lecture 12 slide 12 calls this "the most important comparison": *Adapter protects clients from interface mismatch; Facade protects clients from excessive subsystem knowledge.* At the import boundary we do not have many subsystem classes to coordinate; we have exactly one foreign contract that does not fit ours. That is mismatch, not complexity. Also rejected: changing the core to speak the bank's format — that moves the dependency in the wrong direction and spreads vendor vocabulary (minor units, their date format, their sign convention) through business logic, which is the failure Lec 12 slide 7 describes.

**3. Decorator — the `RepaymentPoster` pipeline (structural) — *new, replaces Strategy***
- *The construction problem:* posting a repayment must sometimes also (a) write an audit journal line naming the operator, (b) record elapsed time for our GQM metrics, and (c) guard against re-posting the same bank reference twice. **Which of these are needed differs per call path, and they combine:** the teller CLI needs audit + timing; the CSV bulk import needs idempotency + timing but one audit entry per *file*, not per row; the EOD engine's internal adjustments need timing only; unit tests need none. Three independent optional behaviours over one base operation.
- *The structure, in the lecturer's roles:* `RepaymentPoster` is the **Component** (`PostingResult post(RepaymentCommand c)`); `CoreRepaymentPoster` is the concrete component holding the real allocation call; `RepaymentPosterDecorator` **IS-A** `RepaymentPoster` and **HAS-A** `RepaymentPoster` (the exact `NotifierDecorator` relationship from Lec 13 slide 5); `AuditLoggingPoster`, `TimingPoster` and `IdempotencyGuardPoster` each add one focused behaviour and then call `wrapped.post(c)`. Composition order is deliberate and documented: the idempotency guard is outermost so a duplicate is rejected before anything is journalled or timed — Lec 13 slide 10's warning that *"the order of Decorators can matter."*
- *Rejected alternative (also covered in the lectures):* **inheritance combinations** — `AuditingPoster`, `TimingPoster`, `AuditingTimingPoster`, `AuditingIdempotentPoster`, … eight classes for three behaviours, and sixteen the day a retry behaviour is added. This is precisely the **subclass explosion** of Lec 13 slide 9 (Logging × Compression × Encryption). Also rejected: **Proxy** — Lec 13 slide 2 separates them by intent, *"Decorator primarily extends behavior; Proxy primarily controls access."* We are not gating access to a protected or expensive object, nor standing in for a remote one; we are layering optional responsibilities that must stack and that vary per instance, so the pressure is Decorator's, not Proxy's.

**Optional extras — only if genuinely needed.** Lecture 12 slide 13 Scenario C and Lecture 11 slide 14 both end with the same warning: *"Patterns solve recurring problems; they should not manufacture problems that did not previously exist."* The rubric also rewards extra patterns **only** where they solve a real design problem. Ranked by how well they can currently be defended:
- **Facade — `RepaymentFacade.postRepayment(...)` (structural, strong candidate for a fourth).** Three clients — the teller CLI, the CSV import and the EOD engine — each need the same five-step workflow in the same order: look up the schedule, allocate through the waterfall, write the ledger pair, re-assess delinquency, publish the alert event. Lec 12 slide 10: *"If ten clients reproduce this workflow, subsystem knowledge becomes distributed."* The Facade exposes one business-level operation, exactly as `CheckoutFacade.checkout()` does. *Rejected alternative:* letting each client call the five subsystem classes itself. **Caveat the team must state unprompted:** per Lec 12 slide 11, the Facade **coordinates and does not absorb** — the allocation rules stay in `RepaymentAllocator`, the classification rules stay in `DelinquencyClassifier`; otherwise it becomes the god class the lecture warns about.
- **Strategy — `ProvisioningPolicy` (behavioural).** Still valid as the OCP mechanism from Week 3 Lecture 1, but note it now varies along a *different* axis from Factory Method: the provisioning percentages are swapped by configuration when the regulator changes them, independently of the loan product. Do **not** also present `InterestCalculator` as a Strategy — it is the Product of the Factory Method above, and claiming both for the same objects invites the question of which pattern is actually in play.
- **State / Observer** — defer to M2. Lecture 11's notes schedule behavioural patterns (Strategy, Observer, Command, Template Method, State, Iterator) for **Week 6**; adding them at M1 means defending vocabulary the class has not been given yet.

### Pattern defence: the one sentence each owner says in the demo

The M1 criterion (15%) and the M2 criterion (20%) both check that the team *"can explain the alternative they rejected and why."* These are drafted in the lecturer's own terms and should be rehearsed verbatim.

| Pattern | Owner | The sentence |
|---|---|---|
| **Factory Method** | **S2** (Schedule Generation) | "`LoanProduct` is the Creator and `generateSchedule()` is the stable workflow — notice it never executes `new ReducingBalanceCalculator()`, it calls `createInterestCalculator()`, and the ConcreteCreator decides which Product comes back; we rejected a Simple Factory because a `create(type)` switch keeps the decision centralised and just moves the conditional ladder, and a method that returns a new object is not automatically the GoF Factory Method." |
| **Adapter** | **S3** (Repayment & Ledger) | "The Client is our posting workflow, the Target is `RepaymentSource`, the Adaptee is the bank's statement reader, and the Adapter converts their minor units into our `Money` and their date format into `LocalDate` — the same dollars-to-cents translation as `LegacyGatewayAdapter`; it's an Object Adapter because it *has-a* reader rather than inheriting one, which keeps all vendor-specific knowledge at one boundary instead of letting it leak into business logic." |
| **Decorator** | **S1** (EOD engine & integration) | "`RepaymentPoster` is the Component, `CoreRepaymentPoster` is the base behaviour, and each decorator both IS-A `RepaymentPoster` and HAS-A `RepaymentPoster`, so we compose audit, timing and idempotency per call path at runtime — we rejected inheritance because three optional behaviours would need eight combination classes, the same subclass explosion as Logging × Compression × Encryption, and we rejected Proxy because we are extending behaviour, not controlling access; order matters, so the idempotency guard wraps outermost." |
| **Facade** *(if used)* | **S3** | "Three different clients needed the same five-step posting workflow in the same order, so `RepaymentFacade.postRepayment()` gives them one business-level operation the way `CheckoutFacade.checkout()` does — and it only coordinates: the allocation rules stay in `RepaymentAllocator`, so it does not grow into a god class." |

### The concurrency feature

**What is concurrent:** the end-of-day engine (M4) processes the active portfolio across a fixed thread pool (`Executors.newFixedThreadPool(4)`), one task per loan account, **while teller threads are still posting repayments** to those same accounts. A demo harness runs both at once; a stress test runs 100 threads posting 1.00 each against one loan.

**The specific race it must prevent:**
1. **Lost update on the outstanding balance.** A teller reads outstanding = 10,000, the EOD task reads 10,000, both write back; one of the two movements vanishes and the ledger no longer balances.
2. **Double penalty application.** If the EOD engine runs twice for the same business date (retry after a failure, or two pool threads picking the same account), penalty is accrued twice — money invented out of nothing.
3. **Torn read in reporting.** The portfolio report sums balances mid-update and reports a total that never existed at any instant.

**Thread-safety strategy (documented, and the one the team defends):**
- **Immutability at the core.** `Money`, `LedgerEntry`, `Instalment` and `ScheduleLine` are immutable value objects; nothing shared is mutable except the account's current position.
- **Per-account striped locking.** A `ConcurrentHashMap<LoanId, ReentrantLock>` (or `Striped` lock array) gives each account its own lock; all balance-changing work happens inside `lock()/unlock()` on that account only, so unrelated accounts proceed in parallel and the lock is never held across I/O.
- **Single lock ordering to avoid deadlock.** Any operation touching two accounts (a transfer between a loan and an advance-credit account) acquires locks in ascending `LoanId` order — a stated, inspectable rule.
- **Idempotency key for the EOD run.** `(loan_id, business_date)` is unique in `delinquency_snapshot`; a second accrual attempt for the same date is rejected by the schema, so penalty double-application is impossible even if the lock discipline is broken.
- **Append-only ledger.** Correctness is verified by an invariant test: Σ debits = Σ credits after every run, regardless of interleaving.
- **Deterministic tests, not sleeps.** Concurrency tests use `CountDownLatch` to force maximal interleaving and assert exact final balances; no `Thread.sleep`, so CI does not flake.

This also satisfies the M3 criterion *"system handles at least two injected or deliberately tested failure scenarios gracefully"*: (a) a repository that throws mid-posting must leave the ledger balanced; (b) an EOD task that dies must not block the pool or leave a half-applied accrual.

### Five quality requirements (ISO/IEC 25010) — **tightened against textbook Ch. 3**

Three changes were made after checking these against Laporte & April §3.3, and the team should be able to say why:

1. **Verification method is now named from the book's closed set.** §3.3 defines *Verifiable* as allowing determination of whether a requirement is met "based on four possible methods — **inspection, analysis, demonstration, or tests**." Each row below names one of those four and then the evidence, instead of describing a tool. This is the single cheapest way to look like the requirement was written by someone who read the standard.
2. **Each requirement is stated in the book's house style for non-functional requirements** — terse, numeric, one measurable claim per line, as in its transit-pass example ("time for reading or coding the pass must not exceed 250 milliseconds; success rate must be 99.99%"). Prose like "shall remain modifiable by a new developer" was unverifiable and has been replaced.
3. **One former "quality requirement" was reclassified.** §3.3 separates requirements into functional, **non-functional (quality)** and **constraints**. "No package cycles between modules" and "`double` is banned" are design/coding **constraints**, not quality requirements; they now sit in SQAP §4.4 (standards, practices and conventions) and QR-04 states the measurable maintainability property instead. Making this distinction explicitly is worth marks on its own, because most teams file everything under "non-functional".

Checked against §3.3's seven characteristics of a good requirement — **necessary, unambiguous, concise, coherent, complete, accessible, verifiable** — each row below carries exactly one measurable claim, uses terminology consistent with the Business Rules Register, and is achievable on a student laptop.

| ID | Requirement | Characteristic | Sub-characteristic | Measurable target | Verification method (§3.3) + evidence |
|---|---|---|---|---|---|
| QR-01 | End-of-day processing of 10,000 active loans shall complete in ≤ 20 s on the demo machine using 4 threads. | Performance efficiency | Time behaviour | Median wall-clock of 5 consecutive runs ≤ 20 s; no single run > 25 s | **Test** — timed JUnit harness over a generated 10k-loan dataset; timings logged to the test report at each milestone |
| QR-02 | Repayment allocation shall reproduce the reference schedules exactly, to the stated monetary scale. | Functional suitability | Functional correctness | 0 discrepancy against 30 hand-computed golden schedules at scale 2, HALF_EVEN; 100% decision coverage of `RepaymentAllocator.post()` | **Test** — golden-file unit tests plus JaCoCo branch-coverage report (this is the instructor's own "100% decision/branch coverage for financial transactions" policy) |
| QR-03 | Any failure during repayment posting shall leave the ledger balanced, with no partial entry. | Reliability | Fault tolerance | For 100% of ≥ 10 injected failure points: Σ debits = Σ credits and 0 orphan entries | **Test** — fault injection via a repository stub that throws at each write point; also **demonstrated** live at the M3 demo as one of the two required failure scenarios |
| QR-04 | Core business-logic methods shall stay within the stated complexity and size limits. | Maintainability | Analysability / Modifiability | Mean cyclomatic complexity ≤ 5; no method > 15; no class > 200 LOC, measured over the five module packages | **Analysis** — SonarQube/Checkstyle metric report produced in CI and attached to each milestone |
| QR-05 | A posted ledger entry shall not be alterable by any component; corrections occur only as compensating entries. | Security | Integrity | 0 mutating members on `LedgerEntry`; 0 `UPDATE` statements against `ledger_entry` in the codebase; 100% of corrections are compensating entries | **Inspection** — the Fagan inspection checklist carries this as an explicit item, supported by a static-analysis rule and a unit test asserting reversal-only correction |

*Trade-off (the Week 3 lecture asks for one): QR-01 against QR-03/QR-05 — finer-grained locking, defensive copying and append-only correction all improve integrity and fault tolerance while costing time behaviour. We resolve it by locking per account rather than globally, and we record the measured cost in the GQM data rather than asserting that it is small.*

### Standards awareness — M1-ready text (SE431 M1 criterion, 10%)

The rubric asks for a *"brief, specific (not generic) discussion of how 12207 / 29110 / CMMI relate to this project's scale."* Lectures 08 and 09 supply the framing; the paragraph below is written to go into the M1 document almost verbatim. It is deliberately short, because the criterion rewards specificity, not length.

> **Standards awareness for MicroLend.** Software has no laws of nature to stop a defect executing — a silent rounding error or a lost update in our ledger meets no physical resistance the way an under-designed arch meets gravity — so for this project we treat process standards as the surrogate laws, and, following Laporte & April §4.2, as an element of **prevention cost** in the cost-of-quality model rather than as paperwork; by the business-model classification in the same section our project is *custom software written in-house*, the one category where standards are adopted voluntarily to stop chronic rework, not imposed by a client. **ISO/IEC/IEEE 12207** (§4.4) tells us *what processes must exist*; of its Supporting processes we genuinely perform Configuration Management (branch protection, `m1`/`m2`/`m3` baseline tags), Verification and Validation, Joint Review and Quality Assurance, while **IEEE 730-2014** (§4.6) tells us *how* the SQA function executes inside that lifecycle — its sixteen activities in three groups (SQA process implementation, product assurance, process assurance), its principle that the depth of SQA is set by the **product risk**, and its rule that any activity we do not perform must be declared *not applicable with a justification* rather than quietly dropped; that rule is why our SQAP carries an explicit non-applicability list (no subcontractors, so "evaluate subcontractor processes for conformance" is marked N/A). Full 12207 and CMMI adoption is wrong at our scale: three students on one project is a **Very Small Entity** under ISO/IEC 29110's 25-person definition (§4.7.6), and by Table 4.4 our ≈ 2 person-month effort puts us in the **Entry** profile, whose two processes — Project Management (planning, execution, evaluation, closure) and Software Implementation (initiation, requirements analysis, architectural and detailed design, construction, integration and tests, delivery) — map one-to-one onto our three milestones, with the free **Deployment Packages** supplying ready-made templates, checklists and traceability tables so we spend our time on the work rather than on inventing document formats. Lightweight here means *scaled to context*, not undisciplined: the book's Peruvian case study (§4.7.6.4, a four-person VSE on a ~900-hour project) kept rework to **18% of total effort** against the Diaz & Sligo CMM Level 2 benchmark of **23.2%**, and we adopt that 18% figure as the target for our own GQM rework metric. Against **CMMI-DEV** (§4.7.1, Table 4.3) we claim **Maturity Level 2 behaviours only**, and we name them: Requirements Management via our RTM, Configuration Management via protected branches and baselines, Process & Product Quality Assurance via the Fagan inspection and static-analysis triage, and Measurement & Analysis via GQM — we explicitly do **not** claim Level 3, because a maturity level describes an organisation's standard processes rather than one project's, and Levels 4–5 statistical process control is meaningless with three people and one data point per milestone. From **ISO 9001:2015** (§4.3.1) we take two things concretely — the **PDCA cycle**, which is literally our milestone loop (plan the milestone, build it, measure coverage and defect density, then change the *process* rather than promising to be more careful), and **risk-based thinking**, which is why our register already records that only one member fully understands the allocation waterfall although nothing has failed yet — while **ISO/IEC 90003** (§4.3.2) is what licenses us to translate ISO 9001's abstract "design and development verification" into the specific software acts we actually perform: code inspection, static analysis, automated unit tests and decision-coverage criteria. Finally, rigor is scaled to consequence: MicroLend is not safety-critical, so the domain standards of §4.8 — DO-178C, EN 50128, ISO 13485 — do not apply and we make no such claim; but because it is a financial ledger we borrow their one transferable idea, that criticality dictates technique, and adopt as a stated SQAP policy **100% decision/branch coverage on the money path** (`RepaymentAllocator`, `DelinquencyClassifier`) together with a coding-standard rule banning `double` for monetary values — our scaled-down equivalent of EN 50128's language restrictions, recorded under IEEE 730's "Standards, practices, and conventions" clause (§4.9) rather than left as an aspiration.

**Why this version scores.** It names *our* artefacts against each standard, it cites the sections the lecturer teaches from, and it says plainly what we are **not** claiming (Level 3; the safety-critical standards). Section 4.9's own checklist for the planning stage is the test to apply to every sentence — *"what specific standards are applicable to this project? … have specific criteria and standards against which all project plans are to be evaluated been identified and shared within the project team?"* — reinforced by Lecture 09's auditor anecdote, *"Show me the evidence."* Anything here the team cannot evidence at M3 should be cut at M1.

> **Where the book and the slides differ — note it, don't hide it.** (1) *Profile.* Lecture 09 presents the **Basic** profile in detail; by the book's Table 4.4 a project of our size (≈ 2 person-months, first project) is formally an **Entry** profile case, Basic being "one project at a time with a single team". We follow the slides and structure our work around the Basic profile's two processes, and state in one line that Entry is the strictly-correct label for our size — showing we read the table rather than copied the lecture. (2) *CoQ categories.* The slides use the PAF shorthand (prevention / appraisal / failure); the book's Table 4.1 breaks prevention into "quality basis definition" and "project and process-oriented interventions", which is the wording to use when itemising our own costs. (3) *IEEE 730's role.* The slides say IEEE 730 governs the SQA supporting process; the book adds the detail worth quoting in the viva — that the **2014 revision moved away from the plan being the cornerstone**, and that the SQAP now appears in a clause plus Annex C of the standard.

### SQAP scope & outline — every mandatory IEEE 730 section with a one-line plan (SE431 M1 criterion, 20%)

The structure below is the SQAP table of contents **as the textbook gives it** (Laporte & April, Figure 4.7, "Table of contents of a SQAP according to the IEEE 730 [IEE 14]"), elaborated section-by-section in Ch. 13 §13.2. Using this exact numbering is the cheapest possible way to satisfy *"all mandatory IEEE 730 sections listed with a one-line plan for each"* — the criterion asks for the list and a plan, not the finished plan, so M1 needs only this table.

| § | IEEE 730 section | One-line plan for MicroLend |
|---|---|---|
| 1 | Purpose and scope | State that this SQAP governs the MicroLend loan-servicing system across M1–M3, covers the five modules and the shared repository, and is tailored to a 3-person VSE. |
| 2 | Definitions and acronyms | Define the terms we actually use — DPD, PAR30, allocation waterfall, EOD, compensating entry, CC, EP/BVA — plus error/defect/failure per ISO 24765, so the document is self-contained. |
| 3 | Reference documents | List the joint-project guidelines, ISO/IEC 25010, ISO/IEC/IEEE 29148, IEEE 730-2014, IEEE 1028, ISO/IEC 29110 Entry/Basic, our Business Rules Register and our RTM, with the mandatory-deliverables checklist in the style of the book's Table 13.1. |
| 4.1 | SQA plan overview — Organization and independence | Name the three members, their module ownership and their rotating SQA-lead role per milestone; state honestly that full organisational independence is impossible in a 3-person team and that we substitute **non-owner review** (no one approves their own PR) plus the **external inspector at M2** required by the guidelines. |
| 4.2 | Overview — Software product risk | Classify MicroLend's product risk as **financial, not safety**, identify the money path (`RepaymentAllocator`, `DelinquencyClassifier`, the ledger) as the high-risk area, and set the depth of SQA activity from that — IEEE 730's core principle that SQA effort is proportionate to product risk. |
| 4.3 | Overview — Tools | Java + JUnit 5, SQLite, Maven/Gradle, GitHub + Actions, JaCoCo for coverage, SonarQube Community / SpotBugs / Checkstyle for static analysis — each with the quality question it answers. |
| 4.4 | Overview — Standards, practices, and conventions | Our coding standard (`Money` only, `double` banned by a Checkstyle rule), commit-message convention (`test(red):` / `feat(green):` / `refactor:`), documentation standard, and the **100% decision coverage on the money path** policy. |
| 4.5 | Overview — Effort, resources, and schedule | The ~39 student-day construction estimate plus ~8 student-days of SQA work per milestone, mapped onto the Week 6 / 10 / 13 milestone dates, with named owners. |
| 5.1.1 | Product assurance — Evaluate plans for conformance | Check at each milestone that the M1 proposal, the test plan and the SQAP itself still match what is being built, and record deviations. |
| 5.1.2 | Product assurance — Evaluate product for conformance | The Fagan inspection of the two money-path classes, plus static-analysis triage, plus black-box and white-box test execution against the requirement IDs in the RTM. |
| 5.1.3 | Product assurance — Evaluate product for acceptability | Define the acceptance gate for M3: all FRs traced to passing tests, ≥ 60% coverage of core logic with 100% decision coverage on the money path, zero open critical defects. |
| 5.1.4 | Product assurance — Evaluate product life-cycle support for conformance | Check the README, setup instructions, schema DDL and architecture docs are current at each baseline — the rubric's "documentation and repository evidence are mutually consistent". |
| 5.1.5 | Product assurance — Measure products | Defect density per module, branch coverage, cyclomatic complexity per method, open/closed defect counts — the metrics our GQM questions consume. |
| 5.2.1 | Process assurance — Evaluate life cycle processes for conformance | Verify that the TDD red-green-refactor sequence, PR review and merge gates were actually followed, using commit and PR history as evidence. |
| 5.2.2 | Process assurance — Evaluate environments for conformance | Confirm every member builds and tests on the same toolchain versions and that CI is the single source of truth for a green build. |
| 5.2.3 | Process assurance — Evaluate subcontractor processes for conformance | **Declared not applicable, with justification:** no subcontractors or third-party components beyond the JDBC driver and test libraries — IEEE 730 requires the declaration, not silence. |
| 5.2.4 | Process assurance — Measure processes | Review/inspection rate (pages or LOC per hour), defect-removal effectiveness by phase, rework as a percentage of total effort (target ≤ 18%, the book's §4.7.6.4 benchmark). |
| 5.2.5 | Process assurance — Assess staff skill and knowledge | Record that concurrency, white-box test design and Fagan inspection are **not yet taught** (see the gap list below) and name the self-study and pairing that closes each gap before it is needed. |
| 6.1 | Additional considerations — Contract review | Treated as review of the joint-project guidelines as our "agreement": a checklist mapping every Section 3.1/3.2 minimum to where it is met, re-checked at each milestone. |
| 6.2 | Additional considerations — Quality measurement | The GQM programme: one Goal, three Questions, and the metrics listed in §5.1.5 / §5.2.4. |
| 6.3 | Additional considerations — Waivers and deviations | Any deviation from this plan is logged with reason, approver and date — e.g., if a planned pattern is dropped after M1, the deviation is recorded rather than silently abandoned. |
| 6.4 | Additional considerations — Task repetition | State when an activity is repeated: a re-inspection is triggered if an inspection finds more than a set number of major defects, and the full regression suite re-runs on every push. |
| 6.5 | Additional considerations — Risk to performing SQA | The honest risks to the QA work itself — exam weeks compressing the inspection, the external inspector falling through, one member carrying the SQA load — each with a mitigation. |
| 6.6 | Additional considerations — Communications strategy | Weekly 15-minute cross-explain session, defect log and CAPA in the repo's issue tracker, milestone status summarised in the M2/M3 documents. |
| 6.7 | Additional considerations — Non-conformance process | How a defect or process non-conformance is raised, triaged by severity, assigned, corrected and verified — the CAPA loop the M2 and M3 rubrics ask to see closed. |
| 7.1 | SQA records — Analyze, identify, collect, file, maintain and dispose | All QA records live in `/qa` in the repository: inspection log, defect log with CAPA status, static-analysis reports, coverage reports, GQM data, risk register versions. |
| 7.2 | SQA records — Availability of records | Records are in the hosted repository and versioned with the code, so any record can be produced on request at the demo — the answer to "show me the evidence". |

**Inspection planning note (for M2, from §5.5 and Figure 5.12).** The book presents the Fagan process as six steps — **IP100 plan inspection → IP110 kickoff meeting → IP120 document checking (individual preparation) → IP130 logging meeting → IP140 edit document → IP150 follow-up, exit and release** — and cites IEEE 1028's guideline rates of **100–200 LOC per hour** for source code (2–3 pages/hour for requirements documents). That rate is the number to plan with: inspecting `RepaymentAllocator` plus `DelinquencyClassifier` at roughly 250 LOC means a logging meeting of about 1.5–2 hours, which is schedulable with an external inspector and which also gives the defect-detection-rate denominator for the GQM programme. Teams that skip the rate end up "inspecting" 800 lines in forty minutes and find nothing, which the defect log then shows.

### Seven starter risks for the register

**Scoring convention (textbook §11.3.1, Table 11.7).** Probability **P** and consequence **C** are each rated 1–5 (1 = not very probable / of little consequence, 5 = almost certain / catastrophic), and **risk exposure E = P × C** gives the priority ordering — so "priority" is computed, not asserted. The guidelines require probability, impact, priority, mitigation, owner, status and residual risk; the P×C column supplies the priority and makes the register auditable. The ratings below are given in the book's words *and* numerically.

| ID | Risk | Prob. | Impact | Priority (E = P×C) | Mitigation | Owner |
|---|---|---|---|---|---|---|
| R1 | **Late start** — M1 is due Week 6 and scope is not yet frozen, so the proposal is written under time pressure and the design is not thought through | High (5) | High (4) | **20** | Freeze scope to the 5 modules and the Business Rules Register in one sitting; the three owners draft their module sections in parallel; no code before the M1 class diagram is agreed | S1 (Team Lead) |
| R2 | **Money rounding defects** — `double` arithmetic or inconsistent rounding makes schedules disagree with the ledger by cents | Medium (3) | High (5) | **15** | One `Money` value object wrapping `BigDecimal` (scale 2, HALF_EVEN); `double` banned by a Checkstyle rule; 30 hand-computed golden schedules as regression tests; BVA on rounding boundaries | S2 |
| R3 | **Flaky concurrency tests break CI** — timing-based tests fail intermittently and the team starts ignoring red builds | Medium (3) | Medium (3) | **9** | Latch-based deterministic tests, no `sleep`; concurrency suite repeated 50× nightly; any flake is logged as a defect, never retried away | S1 |
| R4 | **TDD history not visible** — team writes code first and back-fills tests, losing M2/M3 marks even though coverage is met | High (4) | Medium (3) | **12** | Commit-message convention `test(red):` / `feat(green):` / `refactor:`; `--no-ff` merges, squash-merge disabled in repo settings; a CI check that a milestone's diff contains red-test commits preceding implementation commits | S3 |
| R5 | **Uneven contribution / a member cannot defend their module** — costs 20% individual evidence and possibly more under Section 7 | Medium (3) | High (5) | **15** | Strict module ownership; rotating SQA deliverable lead per the table above; PRs reviewed by a non-owner; weekly 15-minute session where each member explains their invariants to the other two | S1 |
| R6 | **M2 Fagan inspection lacks an external inspector**, which the guidelines make mandatory — the criterion (20% of SE431 M2) cannot be met retroactively | Medium (3) | High (5) | **15** | Pair with another team in the week after M1; book both inspection meetings (ours and theirs) on the calendar before M2 work starts; keep a signed role sheet (moderator, author, reader, inspector) and plan the logging meeting at IEEE 1028's 100–200 LOC/hour rate | S2 |
| R7 | **Designing ahead of the lectures** — concurrency, TDD, white-box test design, static analysis, SCM practice and the Fagan inspection are all required at M2 but have **not yet been taught** (see the gap list below), so the team must self-teach under deadline or guess the expected form | High (4) | High (4) | **16** | Treat the gap list as a study plan with named owners and dates; each gap is closed by a one-page internal note plus a worked example committed to `/qa` before the technique is used in anger; this is also recorded in SQAP §5.2.5 "assess staff skill and knowledge" | S3 |

### Build-effort estimate

| Work item | Student-days |
|---|---|
| Domain core: `Money`, `LoanAccount`, `Instalment`, `LedgerEntry`, exceptions | 4 |
| M1 Origination & eligibility | 3 |
| M2 Product catalogue & schedule generation (3 strategies) | 4 |
| M3 Repayment posting, allocation waterfall, ledger | 5 |
| M4 EOD engine + concurrency + thread-safety tests | 5 |
| Persistence: SQLite schema, repositories, migrations | 4 |
| M5 Reporting, CSV export, alerts | 3 |
| Test suite (TDD throughout — overlaps the above but budget it explicitly) | 8 |
| CI, static analysis, refactoring passes | 3 |
| **Total** | **≈ 39 student-days ≈ 13 days each ≈ 2 days/week per person over 7 weeks** |

SQA deliverables are additional but are mostly documentation of work already done: budget ~8 student-days each for M2 and M3.

**Main risk of the idea itself:** the correctness of the schedule and allocation arithmetic is load-bearing — if the amortisation is wrong, every downstream test and every report inherits the error and the defect is found late and expensively. The mitigation is cheap and must be done at M1: hand-compute 30 schedules in a spreadsheet, commit them as golden files, and write those tests *first*. Secondary risk: the domain tempts scope creep (guarantors, group lending, restructuring, multi-currency) — the Business Rules Register must be explicitly closed at M1 with a "deferred" section.

### Scores

| Dimension | Score |
|---|---|
| Looks complex | **5 / 5** |
| Build simplicity | **4 / 5** (arithmetic is fiddly, nothing else is) |
| Defensibility | **5 / 5** |
| Technique fit (SOLID / patterns / concurrency / SQA artefacts) | **5 / 5** — all three patterns are now formally taught; the SQA artefacts (RTM, SQAP, inspection target, white-box subjects, GQM data) all have obvious homes |
| Testability | **5 / 5** |
| **Total** | **24 / 25** |

---

# Candidate 2 (runner-up) — **SkyHold**: Airline Seat Inventory, Fare & Refund Engine

### One-line pitch
A reservation back end that holds and confirms seats against a capacity-and-overbooking model, prices itineraries through fare-class rules and ancillaries, drives a PNR through its lifecycle, and computes change/cancellation penalties and refunds from fare rules and time-to-departure bands.

**Why it sounds impressive:** "seat inventory control, fare rules engine, overbooking policy, PNR lifecycle" is airline-industry vocabulary. **Why it is simple:** a seat map is a map of immutable records; a fare rule is a lookup table; a refund is a percentage chosen by two comparisons.

### Modules

| # | Module | Core rules | Owner |
|---|---|---|---|
| M1 | **Flight & Seat Inventory** | Hold a seat for 15 minutes then auto-release; a seat is held/confirmed/free; confirmed count ≤ capacity × (1 + overbooking %); exit-row seats refused to passengers under 15 or with infants. **Concurrent module.** | S1 |
| M2 | **Fare & Ancillary Pricing** | Three fare classes (Saver / Flex / Business) × advance-purchase bands (≥ 21 / 7–20 / < 7 days); baggage allowance by class with excess charged per kg in bands; taxes as a percentage plus a fixed levy; infant and child discounts. | S2 |
| M3 | **Booking & Ticketing Lifecycle** | PNR state machine: Held → Confirmed → Ticketed → Flown / Cancelled / NoShow / Expired; each transition has preconditions (cannot ticket an expired hold, cannot cancel a flown segment); group bookings all-or-nothing. | S3 |
| M4 | **Change & Refund Engine** | Refund = fare × band% − fixed penalty − consumed segments; bands by hours to departure (> 72 / 24–72 / < 24 / no-show); Saver non-refundable but taxes always refundable; a change is a cancel+rebook with fare difference and a change fee. | S3 (lead) + S2 |
| M5 | **Manifest & Reporting** | Passenger manifest, load factor, revenue by fare class, denied-boarding list when overbooked. | S1 |

### Fit table (condensed — full detail only if this candidate is chosen)

| Requirement | Where it lives | Stretch? |
|---|---|---|
| ≥ 4 modules | Five above | No |
| SRP / OCP / LSP / ISP / DIP | Pricing, inventory, lifecycle and refunds are separate actors (SRP); new fare class = new rule object (OCP); all `FareRule` implementations honour the same contract, no `instanceof` (LSP); `SeatHolder` vs `SeatReader` vs `FareQuoter` role interfaces (ISP); services depend on repository interfaces (DIP) | No |
| 3 patterns, creational + structural/behavioural | **Builder** (PNR/itinerary), **Adapter** (legacy fare-file importer), **Strategy** (refund policy per fare class) | No |
| Concurrency | Two agents selecting the same seat; hold-expiry sweeper thread | No — best-in-class |
| Defensive programming / exceptions | Typed `SeatUnavailableException`, `HoldExpiredException`, `FareRuleViolationException` at module boundaries | No |
| Persistence | `flight`, `seat`, `hold`, `pnr`, `passenger`, `fare_rule`, `payment` | No |
| TDD ≥ 60% | Pricing and refund are pure functions | No |
| CFG / CC / basis path | `RefundEngine.compute()` CC ≈ 10; `SeatInventory.confirm()` CC ≈ 8 | No |
| EP / BVA | Hours-to-departure at 23/24/72/73; baggage at 19.9/20.0/20.1 kg; advance purchase at 6/7/20/21 days | No |
| All SE431 artefacts | Same shape as Candidate 1 | No |

### The three patterns — **revised after Lectures 10–13; this candidate gained the most**
- **Builder — `PnrBuilder` (creational). Now considerably stronger.** Lecture 11 slides 8–9 teach Builder explicitly and its justification maps onto a PNR almost word for word: *"useful when construction involves many parameters, optional values, or multiple steps… avoids constructors whose meaning becomes difficult to understand… particularly valuable for immutable objects… validation and consistency checks can be centralized before the final object is created."* A PNR assembles from passengers, segments, seats, ancillaries, contact and frequent-flyer details, most of them optional, and must be valid as a whole before it exists — the lecturer's `HttpRequest` example with different nouns. *Rejected:* the **telescoping constructor** problem he names on that slide, and setters-then-validate, which lets a half-built PNR escape and breaks invariant defence. Also rejected: **Factory Method** — per his Lecture 11 slide 9 summary, Factory Method answers *which* product, Builder answers *how to assemble one*; we have one product type with many optional parts.
- **Adapter — `LegacyFareFileAdapter` (structural).** Fare tables arrive in a fixed-width legacy format; the pricing core consumes the `FareSource` interface it owns, and the adapter translates names, dates and minor-unit amounts at the boundary. *Rejected:* **Facade** — per Lecture 12 slide 12, the issue here is a foreign format (mismatch), not excess subsystem knowledge (complexity).
- **Decorator — `FareComponent` for ancillaries (structural) — new, and very natural.** Base fare wrapped by `BaggageFee`, `SeatSelectionFee`, `MealFee` and `TravelInsurance`, composed per booking. Three or four optional charges give 8–16 combinations by inheritance — exactly the subclass explosion of Lecture 13 slide 9 — and order matters because taxes apply to some components and not others. *Rejected:* inheritance combinations; and **Proxy**, because we are extending behaviour rather than controlling access.
- *(Strategy — `RefundPolicy` per fare class remains available as a behavioural fourth after Week 6; it was the third pattern in the earlier draft. Rejected alternative: Template Method, since Saver's "taxes only" policy is not a specialisation of Flex's.)*

### Concurrency
Two agent threads select seat 14A simultaneously; the sweeper thread expires holds concurrently. **Races:** double-booking one seat; confirming past the overbooking cap; releasing a hold that was confirmed microseconds earlier. **Strategy:** the seat map is a `ConcurrentHashMap<SeatId, SeatRecord>` of immutable records updated only by `compute`/`replace` compare-and-set; the confirmed-count check and the increment are done under a per-flight `ReentrantLock` so the cap check and the write are atomic; the sweeper is a `ScheduledExecutorService` that releases only holds whose token still matches. Tested with 50 threads racing for one seat — exactly one must win.

### Five quality requirements
| ID | Requirement | Characteristic | Sub-characteristic | Target | Verification |
|---|---|---|---|---|---|
| QR-01 | A seat is never confirmed to two passengers | Reliability | Maturity | 0 double-bookings in 10,000 concurrent attempts | Concurrent stress test |
| QR-02 | Quote latency stays interactive | Performance efficiency | Time behaviour | 95th percentile ≤ 200 ms per itinerary quote | Timed test harness |
| QR-03 | Refund amounts are exact | Functional suitability | Functional correctness | 0 deviation across 25 golden cases; 100% branch coverage on `RefundEngine` | Golden tests + JaCoCo |
| QR-04 | Expired holds are always released | Reliability | Recoverability | 100% of holds released within 30 s of expiry, including after a simulated crash | Fault-injection test |
| QR-05 | New fare class added without editing tested classes | Maintainability | Modifiability | Adding a 4th class touches 0 existing production files | Measured diff on a spike branch |

### Five starter risks
R1 Late start (High/High) — freeze scope at M1. R2 Concurrency test flakiness (Medium/Medium) — latch-based determinism. R3 **Slides into CRUD** (Medium/High) — the project reads as a booking form unless the fare/refund engine is foregrounded in the proposal; mitigate by making M4 the demo centrepiece. R4 TDD trail invisible (High/Medium) — commit conventions, no squash. R5 Topic over-subscription — several teams will pick airline booking, so the design must be visibly deeper than theirs; mitigate by leading with the fare-rules engine and the overbooking policy, not the booking screen.

### Build effort and main risk
≈ **34 student-days** (slightly cheaper than MicroLend — less arithmetic). **Main risk:** it is the most-chosen topic in this kind of course and, presented carelessly, looks like a CRUD booking app; the rules are also thinner than a loan ledger's, so the SOLID write-up has less to bite on.

### Scores (revised)
Looks complex **4/5** · Build simplicity **5/5** · Defensibility **5/5** · Technique fit **5/5** *(was 4 — Builder and Decorator are now both formally taught and both land naturally here)* · Testability **5/5** — **24 / 25**, now level with Candidate 1 on raw score. See "Does the ranking still stand?" for the tiebreak.

---

# Candidate 3 — **DispenseRx**: Pharmacy Dispensing & Batch-Controlled Inventory

### One-line pitch
A pharmacy back office that validates prescriptions against dosage, age and controlled-substance rules, allocates stock from expiry-dated batches by a configurable policy (FEFO/FIFO), dispenses with a printed label and an audit trail, and manages quarantine, recall and reorder.

**Why it sounds impressive:** patient-safety validation, controlled-drug registers and batch traceability sound regulated and serious. **Why it is simple:** every "clinical" rule is a row in a local table the team defines.

### Modules

| # | Module | Core rules | Owner |
|---|---|---|---|
| M1 | **Prescription Intake & Safety Validation** | Dose within the drug's min/max per kg per day; paediatric and geriatric limits; controlled-substance schedules (II = no refills, 30-day max; III–IV = 5 refills in 6 months); duplicate-therapy and interaction checks against a local pair table; prescriber licence must be current. | S1 |
| M2 | **Batch Inventory & Allocation** | Batches carry expiry, quantity and status (available / quarantined / recalled); allocation is **FEFO** by default, never from an expired, quarantined or recalled batch; partial packs allowed only for loose forms; a recall quarantines every batch of a lot and flags dispensed records. **Concurrent module.** | S2 |
| M3 | **Dispensing & Labelling** | A dispense is atomic across validation, allocation, label generation and audit entry; label content varies by form (tablet/syrup/injection); controlled dispenses write to a separate register that is append-only. | S3 |
| M4 | **Reorder & Expiry Management** | Reorder point = average daily usage × lead time + safety stock; short-dated stock (< 90 days) flagged; automatic purchase suggestions; wastage report. | S2 (lead) + S1 |
| M5 | **Audit, Register & Reporting** | Controlled-drug register, dispensing history per patient, wastage and recall reports, CSV export. | S3 |

### Fit table (condensed)

| Requirement | Where it lives | Stretch? |
|---|---|---|
| ≥ 4 modules | Five above | No |
| SOLID (all five) | Validation rules, allocation, dispensing, reorder are separate actors; new validation rule = new `PrescriptionRule` object (OCP); all rules return a `RuleResult`, none throws to skip (LSP); `StockReader` vs `StockAllocator` vs `BatchAdmin` (ISP); services depend on repository abstractions (DIP) | No |
| 3 patterns | **Factory** (`LabelFactory` per dosage form), **Adapter** (supplier catalogue CSV / barcode-scanner stub), **Chain of Responsibility or Strategy** (validation pipeline / allocation policy) | Chain of Responsibility is **beyond the taught set** — prefer Strategy for allocation policy, which is lecture-blessed |
| Concurrency | Two counters dispensing from the same batch at once | No |
| Persistence | `drug`, `batch`, `prescription`, `prescription_line`, `dispense`, `controlled_register`, `interaction_pair` | No |
| CFG / CC / basis path | `SafetyValidator.validate()` CC ≈ 12; `BatchAllocator.allocate()` CC ≈ 9 | No |
| EP / BVA | Dose at min−0.01 / min / max / max+0.01; expiry at today−1 / today / today+1; refill count at 4/5/6; stock at 0/1 pack boundaries | No |

### The three patterns — **revised after Lectures 10–13**
- **Factory Method — `LabelPrinter` creator hierarchy (creational).** *Problem:* label layout and mandatory warnings differ by dosage form and controlled schedule; the dispensing workflow must not know the layouts. **Caution:** a `LabelFactory.create(form)` holding a `switch` is a **Simple Factory**, which Lecture 11 explicitly distinguishes from the GoF Factory Method — the creator must be a subclass hierarchy (`TabletDispenser`, `SyrupDispenser`, …) overriding `createLabel()` within a stable `dispense()` workflow. *Rejected:* **Builder** — the variation is in *which kind* of label is produced, not in assembling many optional parts of one label; and **Simple Factory**, for the reason above.
- **Adapter — `SupplierCatalogueAdapter` (structural).** *Problem:* each supplier publishes its catalogue in a different CSV shape; the inventory core owns a `CatalogueSource` interface. *Rejected:* writing a parser per supplier inside the inventory service, which couples business policy to foreign formats (Week 2 Lecture 3).
- **Strategy — `AllocationPolicy` FEFO / FIFO / nearest-pack (behavioural).** *Problem:* the pharmacy's allocation policy is configurable and must be swappable without touching the dispensing path. *Rejected:* a boolean flag plus an `if` inside the allocator — it is the conditional ladder the OCP lecture refactors away.

### Concurrency
Two dispensing counters allocate from batch `B-2291` simultaneously while the recall sweeper quarantines it. **Races:** negative stock; two counters issuing the last pack; a pack issued from a batch that was recalled microseconds earlier. **Strategy:** per-batch `ReentrantLock`, batch records immutable and replaced atomically, reservation-then-commit so the quantity is decremented under the same lock that checks it, and a status re-check inside the lock so a recall always wins. Stress test: 40 threads, 10 packs available, exactly 10 succeed and 30 receive `OutOfStockException`.

### Five quality requirements
| ID | Requirement | Characteristic | Sub-characteristic | Target | Verification |
|---|---|---|---|---|---|
| QR-01 | No expired or recalled stock is ever dispensed | Reliability | Maturity | 0 occurrences in 10,000 concurrent dispense attempts including a mid-run recall | Concurrent stress test |
| QR-02 | Safety validation never passes a prescription that violates a rule | Functional suitability | Functional correctness | 100% of 40 EP/BVA cases classified correctly; 100% branch coverage on `SafetyValidator` | Test suite + JaCoCo |
| QR-03 | Dispense is atomic | Reliability | Fault tolerance | 100% of injected failures leave stock and register mutually consistent | Fault injection |
| QR-04 | Controlled-drug register is tamper-evident | Security | Integrity | 0 mutating operations on register rows; 100% corrections by compensating entry | Static analysis + inspection |
| QR-05 | A new validation rule is added without editing existing rules | Maintainability | Modifiability | Adding a rule touches 0 existing production files | Spike-branch diff |

### Five starter risks
R1 Late start (High/High). R2 **Fabricated clinical data is hard to defend** (Medium/High) — a grader asks "where did your interaction rules come from?"; mitigate by declaring the rule table an explicit, cited-as-synthetic project artefact with a documented provenance note at M1. R3 Scope creep into insurance claims / e-prescribing (Medium/Medium) — freeze at M1. R4 TDD trail invisible (High/Medium). R5 Concurrency test flakiness (Medium/Medium).

### Build effort and main risk
≈ **36 student-days**. **Main risk — now worse after SQA Lecture 08 and textbook §4.8.** The domain's credibility rests on invented clinical data, and the one question the team cannot fully answer — "is that dosage limit real?" — is the obvious one to ask in a viva. That was already true; what has changed is that the class has now been *taught* that standalone software used for diagnosis or dosing is legally **Software as a Medical Device** under ISO 13485, that it demands design controls, clinical validation and ISO 14971 risk management, and that "the AI wrote that part" is not a defence. A grader who has just delivered that lecture is very likely to ask where the interaction table came from and what the design history record looks like, and there is no good answer. The rules are also softer than money rules: a loan balance is either right or wrong; a dosage limit is a judgement.

If the team still wants this domain, the mitigation is to state at M1 — in the SQAP's product-risk section (IEEE 730 §4.2) — that the rule tables are **synthetic project data, not clinical data**, that the system is a stock-control and workflow exercise rather than a clinical decision aid, and that ISO 13485 is therefore declared not applicable with a justification. Making that declaration explicitly is defensible; leaving it implicit is not.

### Scores (revised)
Looks complex **5/5** · Build simplicity **4/5** · Defensibility **3/5** *(was 3.5 — ISO 13485/SaMD is now taught material, which raises the probability of the one question the team cannot answer)* · Technique fit **4.5/5** · Testability **4.5/5** — **21 / 25**

---

# Techniques the rubric requires that have **not yet been taught**

This matters for scheduling, not for the choice of idea — it applies identically to all three candidates. As of Lectures 13 (SCD) and 09 (SQA), everything **M1** needs has been taught: problem definition, module decomposition, OOD, all five SOLID principles, pattern selection with rejected alternatives, ISO 25010 quality requirements, the RTM, IEEE 730's SQAP structure, Cost of Quality, standards awareness and risk management. **M1 can be written entirely from taught material** — which is exactly what a late-starting team needs to hear.

**M2 (Week 10) is where the exposure is.** Six of its criteria depend on techniques not yet delivered. The team is therefore designing ahead of the lectures and should log that as a risk (R7 above) and as an SQAP §5.2.5 "assess staff skill and knowledge" entry, rather than discovering it in Week 9.

| Technique the rubric requires | Where it is marked | Taught yet? | What to do |
|---|---|---|---|
| **Concurrency & thread-safety** (locks, immutability, atomics, executors) | SE423 M2 criterion (15%), M3 (10%) | **No** — no SCD lecture has covered it | Highest-value gap: the design (per-account striped locks, immutable value objects, idempotency key) is already specified above; S1 writes a one-page internal note plus a worked `CountDownLatch` stress test before M2 |
| **Behavioural patterns as a family** (Strategy formally, Observer, Command, Template Method, State, Iterator) | Pattern criteria | **Week 6**, per Lecture 11's notes | Do not commit to State/Observer at M1; the three chosen patterns are all already taught |
| **TDD / red-green-refactor discipline** | SE423 M2 (10%), M3 (15%) | **No** | Start the commit convention from the first commit anyway — the evidence cannot be reconstructed later |
| **Refactoring & code smells** (three documented before/after) | SE423 M3 (15%) | Only mentioned in passing in Week 1 | Keep a running `/qa/smells.md` from day one; smells found during the Fagan inspection are the cheapest source |
| **Persistence layer & schema design, CI pipelines** | SE423 3.1 minimums, M3 CI (10%) | **No** | Boring technology; low risk, but budget the time |
| **Fagan inspection: roles, stages, defect log, CAPA** | SE431 M2 (20%) | **No** — Chapter 5 not yet lectured | The textbook §5.5 and Figure 5.12 give the six stages (IP100–IP150) and IEEE 1028 gives the rates; read ahead, because the external inspector must be booked before the technique is understood |
| **Black-box test design (EP, BVA)** | SE431 M2 (15%) | **No** | Straightforward; derive from the Business Rules Register |
| **White-box test design (CFG, cyclomatic complexity, basis path)** | SE431 M2 (15%) | **No** | The second-highest-value gap; it is also the criterion on which this project's domain choice pays off most |
| **Static-analysis tooling and triage** | SE431 M2 (15%) | **No** | Install SonarQube/SpotBugs/Checkstyle in CI at M1 so there is a baseline to triage against |
| **V&V plan (IEEE 1012)** | SE431 M2 (15%) | **No** | Distinguish verification ("are we building it right?") from validation ("the right thing?") and tie each activity to a requirement ID |
| **SCM practices: branching, merge gates, baselines** | SE431 M2 (10%) | **No** | Set up branch protection and the `m1-baseline` tag now; the evidence is the repository itself |
| **GQM measurement programme** | SE431 M3 (15%) | **No** | Start collecting now — M3 requires *actual* project data, not estimates, and data cannot be back-dated |

The pattern across this table is that **four of these gaps produce evidence that can only be generated as the project runs** — TDD commit history, SCM baselines, GQM data and the static-analysis baseline. Those four must be set up at M1 even though they are examined later and taught later. The rest can wait for the lecture.

# Does the ranking still stand?

**Yes — but it is now a tiebreak rather than a clear win, and the reason for the win has changed.**

Before the new lectures, MicroLend led partly because its patterns fitted more naturally than the alternatives'. That advantage has gone: Lecture 11 made **Builder** a formally taught, well-justified choice for SkyHold's PNR, and Lecture 13 made **Decorator** an obvious fit for SkyHold's ancillary pricing — arguably a more textbook-perfect Decorator than MicroLend's posting pipeline. On the construction side the two candidates are now level at **24/25**, and a team that prefers the airline domain can defend it just as well.

MicroLend still wins, on four things the construction scores do not capture:

1. **The SQA half, which is where the differences now live.** SE431's M2 is 35% of the project grade and its white-box (15%) and inspection (20%) criteria together are 35% of that milestone. MicroLend's `RepaymentAllocator.post()` (CC ≈ 11) and `DelinquencyClassifier.classify()` (CC ≈ 8) are two consequential methods on a money path — the basis-path set is worth deriving and the inspection has an obvious, justifiable target at a sane IEEE 1028 rate. SkyHold's `RefundEngine` is good; MicroLend's pair is better, and there are two of them.
2. **An exact oracle.** A loan balance is right or wrong to the cent, so every test has an unarguable expected value and no Q&A question can reach a judgement call. This is also what makes QR-02's "0 discrepancy against 30 golden schedules" a credible quality requirement rather than a hopeful one.
3. **Three separate places where the team can quote the course back.** The SQAP example from SQA Lecture 07 (*"100% decision/branch coverage for financial transactions"*), Lecture 08's own list of consequential failures (*"a banking ledger leaks financial balances"*), and the textbook's §4.2 business-model classification, under which an in-house financial system is precisely the case where standards are adopted voluntarily to kill rework.
4. **Topic over-subscription.** Airline booking is the most-chosen topic of this kind; a grader comparing fifteen proposals will see several. That is not a technical argument, but it is a real one.

**Where SkyHold would now be the better choice:** if the team is genuinely nervous about `BigDecimal` and date arithmetic. That was MicroLend's only weak score (build simplicity 4/5) and it is a real risk (R2). SkyHold trades a point of looks-complex for a point of build simplicity and is otherwise equivalent. **DispenseRx has moved further back** — Lecture 08's ISO 13485 / Software-as-a-Medical-Device material makes its weakest point more likely to be probed, not less.

# Recommendation — build **MicroLend**

### Why it beats the other two

1. **Money is the most defensible domain available.** A loan balance is objectively right or wrong, so every test has an unarguable oracle and no Q&A question can reach a judgement call. DispenseRx fails exactly here (invented clinical limits); SkyHold is fine but its rules are shallower.
2. **It is the only candidate whose white-box work is genuinely interesting rather than manufactured.** `RepaymentAllocator.post()` (CC ≈ 11) and `DelinquencyClassifier.classify()` (CC ≈ 8) are two real, consequential methods on the money path — the CFG, cyclomatic complexity and basis-path set are worth deriving and the Fagan inspection has an obvious, justifiable target. The SE431 M2 white-box criterion (15%) and inspection criterion (20%) are 35% of that milestone, and this is where candidates differ most.
3. **It lets the team quote the lectures back.** The SQA Week 3 SQAP example is literally *"100% decision/branch coverage for financial transactions"*, and the SCD Adapter lecture is a payment adapter. Both the SQAP and the pattern justification can cite the instructor's own worked examples — the cheapest defensibility available.
4. **All five SOLID principles are demonstrable, not just three.** The M1 rubric (20%) demands all five be analysed and ≥ 3 demonstrated through concrete design decisions, and penalises unjustified non-applicability claims. MicroLend has a crisp LSP story (the interest-free hardship loan modelled as a zero-returning calculator rather than a throwing subclass) and a crisp ISP story (the EOD engine denied export capability) — the two principles teams usually fudge.
4a. **Note on pattern fit, after Lectures 10–13:** this is no longer a point of difference. SkyHold's Builder and Decorator are now as well-supported as MicroLend's Factory Method, Adapter and Decorator. Points 1, 2 and 3 — the SQA half — now carry the decision. See "Does the ranking still stand?" above.
5. **Impressiveness per unit of effort is the highest of the three.** "End-of-day accrual engine over an append-only double-entry ledger" costs about 40 lines of real logic and reads like core banking.
6. **The concurrency is doubly motivated** — a batch job *and* interactive postings — which makes "why is this concurrent?" trivial to answer, and the ledger-balance invariant gives a single assertion that proves thread-safety in one line of test code.

SkyHold is a legitimate second choice and is marginally cheaper to build; pick it if the team is nervous about financial arithmetic. DispenseRx should only be chosen if the team genuinely likes the domain, because its defensibility ceiling is lower.

### What the team should be careful about

- **Freeze the rules in writing, this week.** Produce a *Business Rules Register* (BR-01…BR-30, one sentence each) as an M1 annex. Every FR in the RTM traces to a BR. Without it, the rules drift and the RTM rots, which is exactly the "RTM smell" the Week 3 lecture audits for.
- **Golden schedules before any schedule code.** Hand-compute 30 schedules in a spreadsheet, commit them, and write those tests first. This is both the TDD evidence and the defence against R2.
- **`Money` only, never `double`.** Enforce it with a Checkstyle/SpotBugs rule on day one, so the static-analysis report has a real, prevented finding class to discuss.
- **Do not squash-merge.** The guidelines explicitly warn that squash-merging feature branches flattens the TDD commit trail; turn the option off in the GitHub repo settings at M1, not later.
- **Book the external inspector immediately after M1.** The M2 inspection criterion is 20% of SE431's biggest milestone and cannot be recovered after the fact.
- **Resist scope creep.** Guarantors, group lending, restructuring, multi-currency and interest-rate changes mid-loan are all tempting and all out of scope. Put them in a "Deferred" section of the M1 proposal — that section itself reads as engineering maturity.
- **Everyone must be able to answer "what invariant does your module protect?"** in one sentence. Run the 15-minute weekly cross-explain session from R5 — Section 7 makes 20% of each individual mark depend on precisely this, and a student who cannot defend claimed work can score zero on that part.
- **Keep the AI Usage Log from day one** (Section 9). It is mandatory, submitted with every milestone, and trivially easy to lose marks on by back-filling it.
- **Do not call a `switch`-based factory a "Factory Method".** Lecture 11 draws this distinction explicitly and a grader who taught it will test it. If the team ends up wanting a simple factory, call it a **Simple Factory** and say why that was enough — that answer scores; the mislabelled one does not.
- **Set up the four evidence-generating practices at M1 even though they are examined and taught later** — the TDD commit convention, branch protection with baseline tags, the static-analysis baseline, and GQM data collection. None of these can be reconstructed afterwards.
- **Write the SQAP's non-applicability declarations rather than omitting sections.** IEEE 730 requires any activity not performed to be marked not applicable *with a justification*; silence reads as an omission, a declaration reads as tailoring.
