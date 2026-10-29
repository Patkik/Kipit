# Kipit improvement analysis and implementation plan

Prepared 29 October 2026. Status: proposal; application behavior has not been changed.

## 1. Recommended direction

Make Kipit a dependable daily ledger before extending its charts or shortcuts. The highest-value release combines correct and explainable spending guidance, recoverable records, a short expense-entry flow, and user-controlled setup.

Keep the current native Android stack and offline operation. Preserve exact centavos, atomic financial writes, isolated demo data, optional notification capture, and confirmation before third-party captures become transactions. No server, account system, analytics SDK, OCR, camera, or gallery dependency is needed for this plan.

Recommended order:

1. Establish financial regression tests, operation identity, and safe migrations.
2. Unify calculation, preview, savings reservations, and daily reconciliation.
3. Add opening balances, editable budgets, transaction details and corrections, and export/restore.
4. Shorten the entry flow and improve dashboard status, state restoration, and accessibility.
5. Make analytics truthful and efficient, then improve notification capture and system surfaces.

## 2. Review scope and evidence

Reviewed all Kotlin application source files, database entities and queries, manifest, build configuration, both test classes, SOURCE_PLAN.md, IMPLEMENTATION.md, VERIFICATION.md, design-system/MASTER.md, and the project design skill. This is a review of the app in this workspace, not a review of a published production build.

Inspected the already-installed sample app on the available Android emulator: Pulse, Deep Lab, and the entry sheet. No financial transaction was submitted. Screenshots are stored in the current chat's visualization folder. The emulator initially displayed a System UI not-responding dialog; selecting Wait allowed inspection. This alone is not evidence of a Kipit defect.

Verification command: `gradlew.bat assembleDebug assembleDemo testDebugUnitTest lintDebug` with the locally installed JDK 21 and Android SDK supplied through process environment variables. Result: BUILD SUCCESSFUL, 102 actionable tasks, 1 executed and 101 up-to-date. Test XML reports show 8 MoneyTest and 6 PaymentParserTest cases, zero failures/errors. These test results were reused rather than rerun. Lint XML contains 12 warnings, 3 hints, and no errors. Build configuration also emits deprecated Android Gradle option warnings.

Not verified: real provider notification formats, physical-device behavior, TalkBack, large fonts, dark mode, process recreation, day rollover, crash recovery, database concurrency, migrations, or performance. Existing tests are unit tests; they do not prove database transaction correctness. Installed emulator APK inspection was not tied to a verified binary hash of this workspace build.

## 3. What already works well

- Monetary storage uses Long centavos. The Centavos type checks addition, subtraction, and multiplication, and expression parsing uses BigDecimal.
- Commit, transfer, settlement, and impulse decisions use Room transactions. Transaction operation keys have a unique database index.
- Split accounting debits the full payment and records the user's personal portion as category spending. Receivables are not subtracted again from liquid cash.
- Avoiding a purchase reserves existing cash rather than creating an income transaction.
- Personal wallets begin at zero; the separate demo application contains labeled sample history.
- Notification capture is allowlisted and remains pending until reviewed. Raw notification bodies are not persisted.
- Native components, visible labels, a restrained forest palette, and textual chart descriptions provide a useful design foundation.

Retain these properties in every release.

## 4. Technical and design health

These scores are provisional code-review judgments, not accessibility certification or measured performance results.

| Dimension | Score / 4 | Evidence and limitation |
|---|---:|---|
| Accessibility | 2 | Material controls and labels exist; charts expose limited information, validation is incomplete, and large-font/TalkBack behavior is untested. |
| Performance | 1 | Entire history is observed; repeated history scans occur in composition; no benchmarks exist. No measured slowdown is claimed. |
| Responsive layout | 2 | Scrollable native layouts work on the inspected phone emulator; primary entry controls fall below the initial view and larger layouts have no adaptation. |
| Theming | 2 | Main colors have light/dark tokens, but unspecified Material container colors appear lavender in the demo and the widget has a hard-coded light background. |
| Visual structure and anti-patterns | 3 | App-specific hierarchy and restrained styling; oversized ring, repeated introductory copy, and a decorative flow chart dilute practical information. |
| **Total** | **10 / 20** | **Significant work needed; prioritize trustworthy behavior and daily usability.** |

Anti-pattern verdict: mixed. The app has a coherent local finance identity. The observed lavender action surfaces conflict with its forest design direction, and the flow ribbons convey more certainty than their underlying data supports. Keep the native visual language; reduce decoration and finish semantic state design. Do not replace the interface with a marketing-style redesign.

## 5. Findings, ranked by user impact

Severity: P0 blocks normal use or presents demonstrated critical loss; P1 materially affects trust, task completion, or recoverability; P2 creates friction or misleading detail; P3 is polish. There are 18 findings below: 11 P1 and 7 P2. No P0 is asserted from this review.

| ID | Priority | Finding and evidence | User impact | Work package |
|---|---|---|---|---|
| F01 | P1 | OmniSheetModal.kt:24 and Budget.kt:7–9 subtract the entered amount from daily allowance for every mode. The actual allowance formula divides liquid cash over remaining days. | Preview disagrees with the post-save result; income appears to reduce spending room, fee-free transfers appear to consume it, and parking appears to spend cash. | W02 |
| F02 | P1 | ExecuteSurplusSweepUseCase.kt:15–23 reconstructs an opening limit from current allowance and spending; only lastSweepDate and lastStreakDate are stored. The streak is incremented but not reset on a spending day. | Late expenses, missed days, fixed-bill payments, and subsequent income cannot be reconciled reliably. | W03 |
| F03 | P1 | Repository and use cases still use unchecked sumOf, addition, and subtraction for several monetary totals. Entities have no foreign keys; commit does not enforce all type/category/fee/owed combinations. | Extreme totals can overflow; future callers or restored data can introduce inconsistent relationships or unsupported transactions. | W01 |
| F04 | P1 | Wallet/category management has no UI; budgets and Utilities assumptions are seeded. Pulse instructs users to record existing balances as income. | Setup misclassifies existing money as earned income and forces users into budgets they cannot personalize. | W04 |
| F05 | P1 | Transactions have no detail, correction, reversal, or refund workflow; Pulse only displays 15 recent records. | A typo or wrong-wallet payment cannot be repaired through the app. | W05 |
| F06 | P1 | Backup is disabled and export/restore is absent. | Uninstalling or losing a device removes the only copy of the ledger. | W06 |
| F07 | P1 | OmniSheetModal.kt:23 uses remember for the draft; KipitApp.kt saves sheet visibility but keeps the capture object only in remember. | Activity/process recreation can discard an entry or detach a capture review from its source. | W07 |
| F08 | P1 | Sample-app inspection confirms Record and the lower keypad are below the initial view. Five modes, notes, wallet/category chips, macros and batch entry share one scrolling sheet. Macro chips write immediately. | Routine expense entry requires searching/scrolling; accidental macro taps commit money without a correction path. | W07 |
| F09 | P1 | Sweep and impulse reservations use different available-cash checks. The impulse path subtracts existing savings but does not protect unpaid fixed bills. Vault buys from the first wallet and offers no release-reservation action. | A reservation can conflict with bill obligations; purchase funding is implicit; users cannot free reserved cash. | W03/W08 |
| F10 | P1 | AppViewModel.kt:36 writes an entire config object copied from UI state. Other operations update the same row. Initialization/analytics collectors have no explicit error UI. | Concurrent settings/savings writes can overwrite fields; startup or query failures can leave incomplete state without an actionable recovery path. | W01/W09 |
| F11 | P2 | Pulse PaceBar always uses primary green and clamps at 100%. Observed Food spending was ₱3,960 against ₱1,500, displayed green. | Over-budget status is easy to miss. | W07 |
| F12 | P2 | Deep Lab uses fixed-width incoming flow unrelated to income, always divides historical spending by three months, and treats unrecorded past days as no-spend days. | Charts imply evidence the app does not possess. | W10 |
| F13 | P1 | Ghost budget applies a percentage of month-to-date spending and labels it monthly. Its BigDecimal division then calls longValueExact without rounding fractional centavos or catching the exception in composition. | Some totals/percentages can throw during rendering; projections also overstate their evidence. This exception path is source-confirmed, not reproduced on the emulator. | W01/W10 |
| F14 | P2 | Fee inclusion differs across Pulse, preview, burn curve, and flow chart. Search is notes/tags only and errors/empty results have no explicit state. | Users see totals that are difficult to reconcile and cannot efficiently inspect history. | W05/W10 |
| F15 | P2 | KipitDao.transactions observes every transaction; composables repeatedly filter all history. UiState.safe recomputes on access. | Work grows with ledger size and can interfere with typing and scrolling. | W09 |
| F16 | P2 | Services.kt capture sourceKey combines notification key and postTime; parser chooses the first currency amount. Review defaults to the first wallet; macro actions remain available during capture review. | Updated notifications may create duplicate proposals, ambiguous texts may select the wrong amount, and review may use the wrong wallet. These are code-level risks, not provider-reproduced defects. | W11 |
| F17 | P2 | Currency rows have no explicit long-text/large-font adaptation; charts lack navigable per-point values; strings are mostly inline; widget theme is fixed. | Accessibility and readable layouts are not sufficiently established across settings and devices. | W12 |
| F18 | P2 | Widget refresh is scheduled at midnight but not explicitly requested after each financial commit. Time-dependent UI does not share an observable day-change source. | Home screen and foreground views can show old allowance/day status until another event refreshes them. | W09/W11 |

### Concrete examples that should become regression tests

**Preview consistency:** Suppose available cash after reservations is ₱3,000 with three days remaining. Current allowance is ₱1,000. A ₱300 discretionary payment leaves ₱2,700, so the present formula produces ₱900 after save. The current sheet previews ₱700. Preserve one defined calculation model and make preview execute the same rules on a proposed transaction.

**Transfers:** Moving ₱500 from Cash to GCash with a ₱15 fee keeps principal inside the liquid-wallet pool. Only the fee reduces pooled funds. Show source/destination balances and fee separately. Transfers involving a credit account require explicit debt rules rather than assuming all transfers behave alike.

**Split:** Paying ₱500 with a ₱150 personal share and ₱350 owed by a friend should reduce the source wallet by ₱500, add ₱150 personal spending, and create a ₱350 receivable. Later repayment restores ₱350 cash, creates no earned income, and must be visible in history.

**No-spend claims:** An unrecorded day before installation is unknown. A day with no entries today is provisional. Neither should automatically be counted as a confirmed completed no-spend day.

## 6. Target user experience

### First use

Briefly explain that records stay on the device. Let the user name/select wallets and enter opening balances with an effective date. Treat these as opening balance events, excluded from income. Offer editable suggested categories and optional budgets/bills. Ask for permissions only when enabling the related feature. Finish at Today with an explanation of the allowance inputs and a visible Backup option.

Do not require every preference before the first expense. A user can skip optional budgets, meal units, and notification capture.

### Routine expense

Open Log expense → enter amount → select a suggested category and wallet → Record. Keep amount, wallet, category and commit action visible together on compact screens. Notes, calculation operators, splits, transfers and batch entry expand only when requested. Make keyboard entry and the custom keypad alternatives instead of competing input surfaces.

A repeat chip fills a draft with amount/category/wallet; the user records it. Optional immediate repeat logging can follow only after durable duplicate protection and Undo exist. Capture review must never display unrelated immediate-commit macros.

### Today

Suggested display names: Today (Pulse), Insights (Deep Lab), Wallets (Vault), with Settings reachable separately. These are proposed copy changes, not a prerequisite for financial fixes. Preserve recognizable branding if names are retained, but add clear functional subtitles.

Order: daily room and calculation explanation → Log expense → items needing review → category pace/status → recent activity → all history. Make the ring smaller or optional. Say “₱2,460 over budget” when appropriate, and pair color with words. Distinguish income, expense, transfer, repayment and correction in activity rows.

### Wallets and savings

Show wallet details and history, clear liquid cash total, savings reservations and remaining spendable pool. Provide add/edit/archive wallet, category budgets, bill schedules, reserve/release savings, selected-wallet impulse purchase, partial repayment and confirmed settlement. “Skip purchase” and “Reserve cash” are separate choices: declining a purchase must remain possible even if there is not enough cash to reserve its full price.

## 7. Implementation work packages

Effort below is engineering time in working days, including focused tests. Ranges assume one developer familiar with Kotlin/Compose, with design/QA support; they are estimates, not commitments. Each package should be a reviewable change with a passing regression suite.

### W01 — Financial rules, operation identity and migration foundation

Priority P1. Estimate 4–6 days. Dependency: none.

Touch: core/model/Centavos.kt, data/local/entity/Entities.kt, data/local/dao/KipitDao.kt, data/local/KipitDatabase.kt, data/local/AppModule.kt, data/repository/KipitRepositoryImpl.kt, presentation/AppViewModel.kt, test source sets.

1. Format the compressed Kotlin files and organize imports in a separate mechanical change so logic review is readable.
2. Define typed financial commands and results. Validate positive principal, nonnegative fees, existing active wallets, required categories, distinct transfer wallets, and which transaction types allow fees/receivables.
3. Use checked monetary folds and checked intermediate expressions throughout calculation and mutation paths. Convert overflow to a typed validation failure; never partially apply a write.
   Immediately fix sandbox percentage arithmetic: choose and document centavo rounding, calculate outside composition, and handle unavailable/overflow results. For example, 1% of 15,050 centavos is 150.5 centavos; the current longValueExact call throws. With HALF_UP, the displayed scenario saving is 151 centavos. Cover this path before broader analytics work.
4. Allocate a stable operation ID when an action/draft is created and retain it through retries. Add a durable operation record/result so all money mutations, including reservation/release and repayment, have replay protection.
5. Replace whole-config UI writes with field-specific DAO updates or transactionally read/modify/write the current row. Move first-run seeding into one idempotent initializer shared by app and services; do the existence check within its transaction.
6. Plan versioned migrations. Validate existing orphan references first; repair only unambiguous cases, otherwise expose a recovery state. Add foreign keys/indexes with archive/restrict semantics appropriate to historical records. Never cascade-delete financial history.
7. Add Room instrumented integration tests and migration fixtures. Do not use destructive fallback. Retain exported schemas.

Acceptance: duplicate commands have one effect; a batch with one bad entry writes nothing; failed transfers leave both wallets unchanged; overflow rolls back; concurrent settings updates cannot erase savings; current version-1 fixtures migrate without losing balances, transactions, tags or pending items. Unsupported credit/debt mutations are rejected until their semantics are specified.

### W02 — One explainable budget engine and exact previews

Priority P1. Estimate 3–4 days. Dependency: W01.

Touch: domain/usecase/Budget.kt, CommitTransactionUseCase.kt, OmniSheetModal.kt, PulseDashboardScreen.kt, AppViewModel.kt.

1. Write a BudgetBreakdown result: liquid balance, unpaid bill reserve, savings reserve, available pool, period end, remaining days, current rolling allowance, and constraint warnings.
2. Keep the existing rolling allowance model for the first release, but name it explicitly. Preview a typed proposed command through the same domain rules used to commit. Do not estimate impact by subtracting the entered principal from allowance.
3. Return both projected wallet balance and projected allowance. Expense uses actual wallet outflow; split shows wallet outflow and personal expense separately; income adds eligible cash; transfer principal between liquid wallets nets to zero; parking changes neither cash nor allowance.
4. Use the same category spending definition, including the documented treatment of fees, in preview and reporting. A positive valid amount is required before triggering an impulse nudge; the inspected empty draft currently shows one when the category is already over pace.
5. Add a “How this is calculated” breakdown. Present deficits as deficits with the reason, not a reassuring green ring.

Acceptance: projected and actual post-commit breakdown match for all supported modes to the centavo, including bill payments, split remainder, fees, month end, negative available pool and reservation changes. The ₱3,000/three-day example previews ₱900 after a ₱300 discretionary payment.

### W03 — Daily records, safe sweeps and reconciled streaks

Priority P1. Estimate 4–6 days. Dependencies: W01, W02.

Add daily_budget_snapshots, daily_budget_adjustments, day_reconciliations and savings_events with a unique local date/zone identity and stable operation IDs. Store calculation version and input references. Inject Clock and ZoneId into time-dependent use cases. Preserve the local accounting date of recorded transactions; define travel/timezone behavior instead of reinterpreting all history silently.

1. Keep rolling allowance separate from the day's opening target. Persist the target used to judge daily behavior. A revision is explicit when income, bill obligations, or user budget settings legitimately change it.
2. Preview sweep amount before approval. Use half the positive eligible day surplus, capped by unreserved cash after protecting due bills. Display the exact amount and remaining daily room.
3. Track discretionary cash outflow for day-surplus eligibility, including split cash advanced to others; show personal spending separately. Never sweep money still needed for a recorded obligation or already reserved.
4. A sweep is a savings reservation event, not income or a wallet transfer. Support release and record its reason. Replaying the same event has no additional effect.
5. Evening status is provisional. Finalize completed days on the next day; reconcile late/backdated entries, reversals, missing days and skipped worker runs. Compute streaks from daily records, resetting on a spending day. Unknown history does not earn a streak.
6. Run reconciliation on app resume as well as approximate workers. Correct prior day results after late entries; show when a previous sweep now exceeds eligible surplus and offer release. Do not silently fabricate historical targets during migration.

Acceptance: two sweep attempts reserve once; a zero-surplus check does not permanently consume a later valid sweep opportunity; late spending updates day status; missed worker runs are caught up; reserved amounts never exceed the eligible pool at reservation time; skipping a purchase creates no cash; a spending day resets the streak.

### W04 — Opening balances and personalized wallets/budgets/bills

Priority P1. Estimate 4–5 days. Dependencies: W01, W02.

Add Wallets/Category/Bill repositories and focused setup screens. Introduce explicit opening-balance/adjustment events, a ledger start date, editable category limits, and recurring bill definitions/occurrences with amount, due date and payment linkage. Do not equate “remaining category limit” with “unpaid bill.”

Allow wallet add/rename/archive and category add/rename/archive. Prevent unsafe archive when a balance or unresolved receivable needs handling. Keep historical labels available. For now, offer Cash, E-wallet and Bank creation; do not expose credit account setup until debt liability, statement payments and available cash rules are designed and tested.

Acceptance: a ₱5,000 opening balance appears as cash and opening history, with ₱0 earned income; changing Food budget updates pace; paying a linked bill reduces its outstanding reserve once; missing/partial/overdue bills work; archived items remain readable in prior history; setup can resume after interruption.

### W05 — Complete history and safe corrections

Priority P1. Estimate 4–6 days. Dependencies: W01, W04.

Add paged History with date, wallet, type, category, amount and note/tag filters. Add transaction detail showing principal, fee, source/destination, personal share, receivable and operation status. Make all activity types visually distinct and show signed wallet effects.

Implement audited reversals and replacement transactions instead of deleting rows. Undo is a reversal command with its own stable ID. Category/note amendments update the search index and derived records atomically. Refunds link to the original purchase, restore the selected wallet and offset the original spending category without becoming earned income.

Settlement becomes a recorded repayment event with selected receiving wallet, amount, date and confirmation. Support partial and multiple-debtor allocation through repayment rows and an outstanding balance, not just a Boolean. Block or explicitly coordinate reversal of a parent payment when repayments already exist.

Acceptance: reversing a transfer restores both balances and fee effects; correcting a split preserves consistent receivables; partial repayment reduces outstanding balance correctly; a second full-settlement request is harmless; history totals reconcile to wallet balances from their opening anchors; clearing search removes prior results and empty/error states are readable.

### W06 — User-controlled export and restore

Priority P1. Estimate 3–5 days. Dependencies: W01, W04, W05 schema contracts.

Use Android's system document picker. Provide readable CSV history export and a separate versioned full backup format containing financial records, settings, operations and relationships. Raw notification bodies remain excluded. Mark CSV as an export, not a full restore backup. Protect CSV text fields against spreadsheet formula interpretation.

Create a coherent snapshot in a transaction; do not copy only the live database main file while WAL is active. For full backup offer password-based authenticated encryption using a reviewed implementation and documented recovery limitations. Do not invent cryptography. Handle the required libraries within the offline dependency policy.

Restore into a temporary database, check schema/version, record limits, references, operation IDs and financial invariants, then show a preview of wallet totals and record counts. First release supports explicit replacement, not merging. Create a local rollback snapshot before replacement; cancellation, invalid password, corrupt file, insufficient storage or process interruption must preserve the existing ledger. Close/reopen database users and refresh widgets after success.

Acceptance: an export/restore round trip preserves centavos, relationships, timestamps, reservations and duplicate protection; corrupt/incompatible files make no changes; demo backups cannot be silently restored into a personal ledger; backup age is visible; encrypted backups cannot be read with an incorrect password.

### W07 — Fast entry, clearer dashboard and restored drafts

Priority P1. Estimate 3–5 days. Dependencies: W02 and W05 for Undo.

Touch: OmniSheetModal.kt, PulseDashboardScreen.kt, KipitApp.kt, Theme.kt. Add EntryViewModel and SavedStateHandle for small draft primitives/IDs. Restore capture by ID from Room, never a transient object. Store larger explicit drafts in Room only if needed. Android's guidance distinguishes saveable UI state from durable application data: [Save UI state in Compose](https://developer.android.com/develop/ui/compose/state-saving).

1. Start with a compact Expense form; move Income/Transfer/Split/Park into an obvious secondary mode chooser. Put amount, wallet/category summary, keypad and sticky Record action in a bounded layout with keyboard/navigation insets handled.
2. Reveal notes, calculator operators and batch input on demand. Batch has a parsed review list, unknown-wallet/category errors and totals per wallet. Support aliases/multiword names and unambiguous thousands separators; do not silently assign unknown categories to the first category.
3. Repeat chips prefill a draft. Capture review locks unrelated modes/actions and explicitly selects the provider's wallet or asks the user to choose. Show principal plus fee and source/time before committing.
4. Add field-level errors and specific success feedback with Undo. Preserve values on failure. Validate destination when source wallet changes. Prevent double-submit while preserving stable operation identity.
5. Show explicit category statuses: On pace, Ahead of pace, Over budget, or No budget set. Include remaining/over amount. Reduce ring height and let the user inspect allowance components.
6. Complete Material color roles for primary/secondary/tertiary containers and surfaces so default lavender does not leak into controls. Retain system fonts and established forest/coral semantics.

Acceptance: a standard expense can be entered and committed without scrolling the entire form on the agreed compact-phone layout; at 200% font scale all essential controls remain reachable without clipping; draft survives recreation; a capture remains attached after process recreation; blank amount produces no impulse warning; over-budget categories show text plus a distinct semantic state.

### W08 — Practical impulse and savings controls

Priority P1. Estimate 2–3 days. Dependencies: W03, W05, W07.

Add separate Skip and optional Reserve actions; show available reservation capacity. Offer partial reservation if desired. Buying opens a prefilled expense review with a selected wallet and current affordability. Show status history for skipped/purchased items rather than making all completed items disappear. Add goal progress and reserve/release history. Keep goal setting independent of whether the target is currently attainable.

Acceptance: declining an unaffordable item still succeeds; skipping without reservation changes no cash or savings; reserve/release changes only reservations; purchase debits the chosen wallet exactly once; cooling-off status updates on resume without needing navigation.

### W09 — Screen state, queries and time refresh

Priority P1. Estimate 3–5 days. Dependencies: W01; coordinate with W07.

Split AppViewModel into Today, Entry, Insights, Wallets and Settings state owners, keeping domain logic in shared use cases. Depend on repository interfaces; avoid presentation access to concrete database/DAO internals. Replace the global busy flag with command-specific operation status and explicit loading/ready/empty/error states.

Use bounded date-range and aggregate DAO queries for daily/monthly totals and charts. Page history; stop observing all transactions on every screen. Compute aggregates once per input change outside composition. Use background dispatchers for meaningful CPU-heavy transforms. Debounce search, cancel old requests, catch failures and expose no-results states. Circular hour distance should handle 23:00 and 00:00 in category predictions.

Introduce a shared observable accounting date plus resume/timezone-change handling. Recalculate allowances/insights on relevant events and invalidate widgets after committed writes. Avoid collecting separately updated tables into a mixed financial snapshot where displayed balances and transactions belong to different commits.

Acceptance: a new transaction invalidates only relevant data; a 10,000-record synthetic ledger does not cause full-history UI scans; date-dependent values change while the app stays open overnight; a query failure has Retry; rapid search edits cannot show stale results. Measure startup/scrolling with [Android Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview); set performance thresholds after establishing a physical-device baseline.

### W10 — Honest, actionable insights

Priority P1 arithmetic fix in W01; remaining analytics P2. Estimate 3–5 days. Dependencies: W02, W03, W04, W09.

First replace the decorative Sankey with labeled category and wallet totals plus reconciliation. A full wallet-attributed Sankey is optional and follows only when opening balances, transfers, repayments and fees can be represented without misleading flows.

Historical comparison counts only eligible observed periods and labels coverage, for example “Average of 2 complete months.” Show “Not enough history” when appropriate. No-spend calendar cells distinguish unknown, confirmed, provisional today, spending and future states; expose day details. All charts use consistent personal-spending/fee definitions and supply values, units, date ranges and accessible alternatives.

Sandbox uses per-category reductions and a clear baseline from completed observed months or explicit user input. Projected savings remain hypothetical; show assumptions and exclude fixed obligations unless edited intentionally. Offer a goal-date estimate only when savings contribution assumptions exist. Keep all scenario changes in memory and verify no ledger mutation.

Acceptance: zero income produces no apparent income ribbon; one observed month is not averaged against two invented zero months; two categories' reductions add exactly to the displayed scenario saving; fee totals reconcile; days before tracking are unknown; projections state their assumptions and never modify Room.

### W11 — Conservative capture, useful reminders and fresh widgets

Priority P2. Estimate 3–4 days. Dependencies: W01, W02, W07, W09.

Split Services.kt into parser adapters, notification listener, notifier, scheduling, workers, widget and tile files. Add user opt-in, per-provider switches, current permission status, reminder preferences and disabled-state explanation.

Provider-specific parsers consume title/text/bigText deliberately; classify outgoing/incoming/refund/failed/unknown messages and reject ambiguity. Build sanitized local test fixtures without persisting raw notification text. Use provider transaction reference when available for stable deduplication. Otherwise flag likely duplicates for review using a bounded fingerprint; do not silently suppress legitimate identical purchases. Persist capture states so dismissal/commit cannot be replayed as a new proposal by an update.

Deep-link reminders to the relevant capture/sweep/insight rather than a generic home screen. Refresh widget after writes and on resume/day change; show “Updated…” and privacy options. Tile opens the entry draft. Keep lock-screen financial writes out of this release. Use approximate WorkManager timing honestly and test denied permissions, reboot, delayed work and retry.

Acceptance: the same capture commits once across retries; notification updates are handled in fixtures; identical legitimate purchases can both be recorded; ambiguous or balance-first texts do not create an auto-approved amount; permission denial never blocks manual logging; widget eventually reflects committed balances and states when it was updated.

### W12 — Accessibility, responsive layouts and release verification

Priority P2. Estimate 3–5 days. Dependencies: packages affecting each tested surface.

Add 48dp minimum Android touch targets, semantic control labels/states, clear focus order, headings, and error associations. Include chart value tables/day details and explicit progress/status descriptions. Audit contrast of actual foreground/background pairs in both themes; do not infer contrast from color names. Localize strings through resources and retain exact currency formatting. Handle long notes, long wallet names and large money values without hiding important amounts.

Test compact phones, landscape, split screen and tablet widths. Use a navigation rail and constrained content where appropriate; do not stretch a phone sheet indefinitely. Respect system animation settings and avoid announcing every keystroke as a full changing balance.

Maintain an offline release checklist: APK permission/dependency inspection, debug/demo isolation, migration tests, backup restore drills, release signing configuration, lint review, and physical-device critical-flow tests. Never include keys or real financial records in source control. Remove deprecated build flags only in a separate verified build-tool change; do not upgrade every dependency as part of a UI patch.

Acceptance: TalkBack completes logging, review, repayment and restore preview; at 200% text and narrow widths no primary task is blocked; dark/light controls match semantic colors; charts have equivalent readable values; a physical-device report records device/OS/build and observed results. Finish with a polish pass after behavior checks.

## 8. Data contracts and migration sequence

Suggested additions, finalized in W01 before implementation:

| Data | Purpose | Key invariants |
|---|---|---|
| financial_operations | Stable command identity and committed result | Unique operation ID; result and money write commit together. |
| opening/adjustment/reversal/refund events | Explain and correct balances | Explicit type and links; opening money excluded from earned income. |
| daily_budget_snapshots + adjustments | Target used for daily judgments | One accounting date/zone identity; versioned inputs; changes are traceable. |
| day_reconciliations | Confirmed/provisional/unknown daily status | Repeatable reconciliation; late entries invalidate affected dates. |
| savings_events | Reservation/release and sweep history | No cash creation; unique operation; reservations reconcile to total. |
| bill_definitions + occurrences + payment_links | Actual scheduled obligations | Partial payments supported; a payment cannot discharge the same liability twice. |
| receivable_allocations + repayments | Multiple debtors and partial payments | Outstanding = allocated amount minus valid repayments; never negative. |
| capture review status/fingerprint | Reviewed/dismissed/committed lifecycle | No raw body storage; deduplication does not silently lose real payments. |

Use additive migrations first and backfill known facts only. Existing wallet balances may lack a complete opening-history trail; preserve the balance and create a clearly labeled migration opening anchor derived from the stored balance and known wallet effects. Legacy settlements lack repayment rows: reconstruct only where amounts/links are known, label migrated events, and report ambiguity. Never pretend an inferred opening date or daily budget was observed.

Do not remove old fields until backfill and reconciliation have been validated. Test every supported upgrade path, including tags/FTS and demo fixtures. Android documents why migrations must preserve existing on-device records and how exported schemas support verification: [Migrate your Room database](https://developer.android.com/training/data-storage/room/migrating-db-versions).

## 9. Delivery sequence and scope control

| Release | Packages | User-visible outcome | Exit gate |
|---|---|---|---|
| A — Trustworthy calculations | W01, W02, W03 | Correct previews, safe reservations, explainable day status | Database rollback/replay tests; calculation and day-reconciliation matrix passes. |
| B — Usable personal ledger | W04, W05, W06 | Personal setup, full history, corrections and backup | Opening-balance reconciliation; correction/repayment tests; verified restore drill. |
| C — Fast daily use | W07, W08, W09 | Short entry flow, persistent drafts, responsive screen state | Recreation/overflow/input tests; overnight refresh; benchmark baseline. |
| D — Clear insights and integration | W10, W11, W12 | Truthful charts, conservative capture, accessible/device-verified release | Provider fixtures; permission/worker matrix; TalkBack and physical-device evidence. |

Allow approximately 39–59 engineering days for the full scope, with QA/design and device testing scheduled alongside the work. A narrower first release can defer multi-debtor splits, partial repayments, category sandbox sliders, tablet composition and full Sankey; it must retain correct calculations, stable commands, a basic correction path and safe export/restore. Do not ship financial mutations solely because the APK compiles.

Do not start cloud sync, social features, AI recommendations, OCR, investment tracking, automatic lock-screen spending or a framework rewrite during these releases. Biometric entry and app-level database encryption are a separate privacy enhancement after the recovery design is established; neither substitutes for backup.

## 10. Required verification matrix

| Area | Minimum scenarios | Required result |
|---|---|---|
| Money | decimal inputs, rounding boundaries, largest supported amounts, aggregate overflow, negative available pool | Exact centavos; typed error and no mutation on invalid/overflow input. |
| Transfer | fee/no fee, same wallet, missing/archived destination, insufficient cash, duplicate operation, concurrent calls | Atomic balances; only valid fee is external cash loss; one committed operation. |
| Split/repayment | uneven centavo split, zero personal share rejection, partial/multiple repayment, wrong wallet, replay, reversed parent | Full debit once; personal spending accurate; receivable and cash reconcile. |
| Budget | bill payment, partial bill, income, savings reservation/release, month end, February/leap year | Preview equals committed outcome; no obligation counted twice. |
| Daily behavior | before/after midnight, late expense, missed worker, spending after sweep, gap day, timezone change | Repeatable status and streak reconciliation; unknown history stays unknown. |
| Capture | allowlist, spoofed package, balance-first text, fee-first text, refund/failed/OTP, repeated updates, equal real purchases | Unsupported/ambiguous inputs remain uncommitted; no replay; legitimate equal payments can both exist. |
| Entry | empty/incomplete expression, division by zero, batch parse failure, unknown alias, process death, rapid taps | Clear validation; values retained; no partial batch; one commit. |
| Restore | full round trip, corrupt file, bad password, incompatible version, low storage, cancel, interrupted replacement | Original ledger survives failure; valid restore preserves all invariants. |
| UI | TalkBack, 200% font, dark/light, landscape, compact/tablet, long labels, keyboard visible | Primary controls reachable; amounts/statuses readable; charts have equivalent text. |
| Scale | empty, 100, 10,000 and 100,000 synthetic transactions | Record query/startup/frame/memory measurements; no unbounded history scans in composition. |

Financial invariants should be checked after every integration scenario: balances equal opening anchors plus valid wallet effects; transfer principal nets to zero across liquid accounts; receivables are not double-subtracted; repayment is not earned income; reservation/release never creates cash; no duplicate operation has a second effect; FTS points to valid searchable records.

## 11. Success criteria

Correctness: preview/commit equality to one centavo across supported operations; zero partial writes in forced-failure tests; zero duplicate effects under replay; all supported upgrade/restore fixtures preserve balances.

Usability: with 5 representative users, at least 4 can set an opening balance, log a routine expense, find/correct it, explain today's allowance and export a backup without assistance. Measure familiar repeat-expense speed against the current build; aim for a three-second prepared repeat flow, while treating that as a target requiring device measurement.

Trust: every savings claim and projection exposes its basis; unobserved days/months are labeled; current wallet, history and reservation totals reconcile; permissions remain optional.

Performance: establish targets from a declared low/mid-range physical test device, then enforce no regression in entry responsiveness, startup and scrolling. No universal 120 FPS or sub-millisecond query claim.

## 12. Suggested design follow-through

Use /harden for validation, drafts and recovery; /clarify for allowance/status language; /onboard for opening balances; /distill and /arrange for the entry form; /normalize for Material color roles; /optimize for measured query/render work; /adapt for screen/font sizes; then /audit and finally /polish. These are supporting workflows, not substitutes for the financial and migration acceptance gates above.

You can ask for these work packages one at a time, all at once, or in another order. Re-run /audit after fixes to reassess the provisional score.
