This comprehensive, single-shot master prompt is engineered for autonomous AI coding agents (such as Claude Code, Cursor Composer, Windsurf, or Android Studio Gemini) and targets the modern native Android stack, including Kotlin 2.2+ (K2), Jetpack Compose 1.12+, and Room 2.8.5. It consolidates the complete Clean Architecture blueprint, SQLite schemas, deterministic parsers, system services, and the 4-stage behavioral feedback loop into an unambiguous specification with zero OCR bloat.
Copy the entire block below and paste it directly into your AI coding assistant:
Markdown
````
# MASTER IMPLEMENTATION PROMPT: "Kipit" — Local-First Behavioral Budget Tracker for Android

## 1. ROLE & CORE OBJECTIVE
Act as a Principal Android Architect and Lead Kotlin Engineer. Build **"Kipit"**, a 100% offline, local-first personal finance and behavioral budget tracker for Android. 

**Core Philosophy:**
1. **Zero OCR / Zero Bloat:** Do NOT include any camera, gallery, or ML Kit OCR dependencies. All expense logging must be deterministic and take 1 to 3 seconds.
2. **Active Behavioral Intervention:** Every logged centavo feeds a 4-stage behavioral loop (Point-of-Entry Friction, Midnight Pace Recalibration, Weekly SQLite Trigger Diagnostics, and 9:00 PM Positive Surplus Sweeps) that actively prevents overspending before it happens.
3. **Zero-Allocation Currency Precision:** Never use `Float` or `Double` for currency. Use a Kotlin `@JvmInline value class Centavos(val value: Long)` across the entire codebase (`₱150.50` = `15050L`).

---

## 2. TECH STACK & COMPILER SPECIFICATIONS
- **Language:** Kotlin 2.2+ (K2 Compiler enabled)
- **UI Framework:** Jetpack Compose 1.12+ (Material 3, Edge-to-Edge, custom `Canvas` `DrawScope` charts for 120 FPS rendering, Compose Navigation 3)
- **Architecture:** Local-First Clean Architecture (`presentation`, `domain`, `data`, `service`) + Unidirectional MVI (`UiState`, `UiIntent`, `UiEffect` via `StateFlow` and `Channel`)
- **Database:** Room 2.8.5+ with SQLite Write-Ahead Logging (`JournalMode.WRITE_AHEAD_LOGGING`) and **FTS5** (`@Fts4`/`FTS5` virtual table for sub-2ms `#tag` and note search)
- **Dependency Injection:** Dagger Hilt with KSP (Kotlin Symbol Processing)
- **System Integrations:**
  - `Jetpack Glance` (Home Screen Widget with Safe-to-Spend + 1-Tap Habit Macros)
  - `TileService` (Android Quick Settings Shade Tile for 1-Tap Macro logging)
  - `NotificationListenerService` (Offline deterministic Regex capture for e-wallet/bank push notifications)
  - `Jetpack WorkManager` (Midnight Recalibration, 9:00 PM Surplus Sweep, and Weekly Behavioral Diagnostics)

---

## 3. PACKAGE STRUCTURE
Generate the project following this strict modular package structure under `com.kipit.app`:
```text
com.kipit.app/
├── core/
│   ├── model/              # @JvmInline value class Centavos, Enums (WalletType, TxType, LoanStatus)
│   └── util/               # InlineMathEvaluator, MicroSyntaxLexer, TimeWindowHelper
├── data/
│   ├── local/
│   │   ├── entity/         # Room Entities (Wallet, Category, Transaction, SplitReceivable, ImpulseItem, TagFts)
│   │   ├── dao/            # KipitDao (CRUD + Chrono-Contextual Ranker + Behavioral Diagnostic Queries)
│   │   └── KipitDatabase.kt
│   └── repository/         # KipitRepositoryImpl
├── domain/
│   ├── repository/         # KipitRepository interface
│   └── usecase/
│       ├── EvaluateKeypadImpactUseCase.kt       # Stage 1: Real-time keypad consequence preview
│       ├── CalculateSafeToSpendUseCase.kt       # Stage 2: Rolling daily safe allowance & recovery plan
│       ├── RunBehavioralDiagnosticsUseCase.kt   # Stage 3: Temporal triggers, micro-leaks, velocity forecast
│       ├── ExecuteSurplusSweepUseCase.kt        # Stage 4: 9 PM unspent sweep to savings envelope
│       └── CommitTransactionUseCase.kt          # Atomic double-entry transfer, split IOU, & expense commit
├── service/
│   ├── EWalletNotificationListener.kt           # Regex capture for GCash, Maya, BDO, BPI, Grab, Foodpanda
│   ├── SurplusSweepWorker.kt                    # 9:00 PM WorkManager interactive notification
│   ├── QuickMacroTileService.kt                 # Quick Settings Tile
│   └── KipitGlanceWidget.kt                     # Jetpack Glance Home Screen Widget
└── presentation/
    ├── theme/              # Dynamic Material 3 Theme + Financial semantic colors
    ├── omnisheet/          # Surface 1: Swipe-to-Commit Omni-Pad Modal + MVI ViewModel
    ├── pulse/              # Surface 2: Pulse Dashboard Screen + MVI ViewModel
    ├── deeplab/            # Surface 3: Deep Lab Analytics & Ghost Sandbox Screen + Canvas Charts
    └── vault/              # Surface 4: Wallets, Envelopes & Impulse Purgatory Screen

````
1. Centavos(val value: Long): Implement @JvmInline value class Centavos(val value: Long) with operator overloads (+, -, *, /, compareTo), formatting helper (toPhpString()), and a Room @TypeConverter converting Centavos <-> Long.
2. WalletEntity (wallets):
   - id: Long, name: String, type: WalletType (CASH, E_WALLET, BANK, CREDIT), balanceCentavos: Long, isArchived: Boolean.
3. CategoryEntity (categories):
   - id: Long, name: String, iconName: String, colorHex: Long, monthlyLimitCentavos: Long, isDiscretionary: Boolean, isFixedBill: Boolean.
4. TransactionEntity (transactions):
   - id: Long, type: TxType (EXPENSE, INCOME, TRANSFER), amountCentavos: Long, feeCentavos: Long (default 0), sourceWalletId: Long, destinationWalletId: Long?, categoryId: Long?, timestampEpochMs: Long, hourOfDay: Int (0..23), dayOfWeek: Int (1..7), isWeekend: Boolean, note: String, tagsCsv: String.
   - Create indices on (timestampEpochMs, type), (categoryId, timestampEpochMs), and (hourOfDay, isWeekend).
5. SplitReceivableEntity (split_receivables):
   - id: Long, parentTransactionId: Long, debtorName: String, amountOwedCentavos: Long, isSettled: Boolean, createdAtMs: Long, settledAtMs: Long?.
   - Rule: When logging a split bill (e.g., ₱500 total, ₱350 owed by friends), TransactionEntity records ONLY the user's personal burn (₱150) under the expense category so analytics stay pure, while SplitReceivableEntity tracks the ₱350 receivable asset deducted from the source wallet.
6. ImpulseVaultEntity (impulse_vault):
   - id: Long, itemName: String, amountCentavos: Long, categoryId: Long, createdAtMs: Long, unlocksAtMs: Long (default +7 days), status: ImpulseStatus (COOLING_OFF, SKIPPED_AND_SAVED, PURCHASED).
7. UserConfigEntity (user_config):
   - id: Int = 1, mealAnchorCentavos: Long (default 7500L = ₱75.00), savingsGoalCentavos: Long, lockedSavingsCentavos: Long, currentNoSpendStreak: Int.
Implement these exact SQL queries:
1. Chrono-Contextual Category Predictor (Sub-1ms Ranking): Scores categories over the last 30 days using a 3.0x weight if ABS(hourOfDay - :currentHour) <= 1 AND isWeekend = :isWeekend, 2.0x if within 2 hours, and 1.0x otherwise:
   SQL
   ```
   SELECT categoryId FROM transactions
   WHERE timestampEpochMs >= :thirtyDaysAgoMs AND type = 'EXPENSE' AND categoryId IS NOT NULL
   GROUP BY categoryId
   ORDER BY SUM(
       CASE
           WHEN ABS(hourOfDay - :currentHour) <= 1 AND isWeekend = :isWeekend THEN 3.0
           WHEN ABS(hourOfDay - :currentHour) <= 2 THEN 2.0
           ELSE 1.0
       END
   ) DESC
   LIMIT 6
   
   ```
2. Top 5 "Habit Macros" Detector: Finds the 5 most frequent exact (amountCentavos, categoryId, sourceWalletId, note) combinations over the last 30 days so the user can log recurring commute/meal expenses in 1 tap:
   SQL
   ```
   SELECT amountCentavos, categoryId, sourceWalletId, note, COUNT(*) as freq
   FROM transactions
   WHERE timestampEpochMs >= :thirtyDaysAgoMs AND type = 'EXPENSE'
   GROUP BY amountCentavos, categoryId, sourceWalletId
   HAVING freq >= 2
   ORDER BY freq DESC
   LIMIT 5
   
   ```
3. Temporal & Situational Trigger Finder (Stage 3 Diagnostic): Groups discretionary expenses by dayOfWeek and 3-hour time buckets (hourOfDay / 3) to expose high-burn time windows (e.g., Thursday/Friday 9 PM–Midnight).
4. The "Micro-Leak" Radar Query (Stage 3 Diagnostic): Selects high-frequency expenses where amountCentavos <= 15000 (₱150.00) grouped by categoryId and note, returning transaction count and cumulative centavos lost to small habits.
Write a pure-Kotlin recursive descent or Shunting-Yard evaluator supporting +, -, *, / on keypad strings (e.g., "120 + 45 * 2" -> Centavos(21000L)). Must never throw unhandled exceptions on incomplete trailing operators (e.g., "120 + " evaluates safely to 12000L).
Parse comma-separated batch input strings without regex overhead:
- Example input: "15 fare, 120 lunch gcash, 350 dinner split:2 #project"
- Rules:
  - First number token = PHP amount.
  - Matches wallet names (gcash, maya, cash, bdo, bpi) against loaded WalletEntity list.
  - Matches split:N modifier -> divides total by N, sets personal expense to Total / N, and creates a SplitReceivableEntity for Total - (Total / N).
  - Extracts #tag tokens into tagsCsv.
  - Matches remaining keywords against category names or saves as note.
Executed synchronously on every keystroke in the Omni-Pad:
- Inputs: typedAmount: Centavos, currentSafeToSpendToday: Centavos, mealAnchor: Centavos, categorySpentMonth: Centavos, categoryLimitMonth: Centavos, isDiscretionary: Boolean, monthProgressRatio: Float (0.0..1.0).
- Outputs LiveKeypadConsequence:
  - newSafeToSpendToday: Centavos (currentSafeToSpendToday - typedAmount)
  - mealEquivalent: Float (typedAmount / mealAnchor)
  - categoryUtilizationAfter: Float
  - paceDeltaRatio: Float (categoryUtilizationAfter - monthProgressRatio)
  - triggerImpulseVaultNudge: Boolean (true if isDiscretionary AND (newSafeToSpendToday < 0 OR paceDeltaRatio > 0.15f)).
- Formula:SafeToSpendToday = (Sum(LiquidWallets) - UnpaidFixedBillsThisMonth - LockedSavings - UnsettledReceivables) / DaysRemainingInMonth
- Adaptive Recovery Calculation: If yesterday's spend exceeded yesterday's safe limit by OverspentAmount, compute DailyRecoveryTrim = OverspentAmount / min(7, DaysRemainingInMonth) and surface a calm recovery message showing how trimming DailyRecoveryTrim over the next 7 days keeps the monthly savings goal 100% intact.
- Filter strictly for package names: com.globe.gcash.android (GCash), com.paymaya (Maya), com.bdo.retail (BDO), com.bpi.ng.app (BPI), com.grabtaxi.passenger (Grab), com.global.foodpanda.android (Foodpanda).
- Use pre-compiled Kotlin Regex patterns to extract:
  - amountCentavos (e.g., matching (?:PHP|₱|P)\s?([0-9,]+\.[0-9]{2}))
  - feeCentavos (if present)
  - merchantOrRecipient
- Immediately post a high-priority local notification with 3 PendingIntent action buttons corresponding to the top 3 Chrono-Contextual Predicted Categories so the user can categorize and commit the expense directly from the lock screen in 1 tap.
- Scheduled daily at 21:00 via WorkManager.
- Queries today's discretionary spend vs. SafeToSpendToday.
- If Surplus > 0: Posts an interactive notification: "You finished ₱X under today's pace! Sweep 50% (₱Y) into your Savings Lock and roll over 50% to boost tomorrow's allowance?" with a 1-tap [Sweep & Boost] action button.
- If DiscretionarySpend == 0: Increments currentNoSpendStreak and marks today green on the heatmap.
1. Top Live Consequence Header: Animates smoothly on every keystroke (AnimatedContent), displaying:
   - Safe Today: ₱280 → ₱160 (turns amber/coral if negative)
   - Equivalent to 1.6 budget meals
   - Category Velocity mini progress bar with a vertical tick mark for Today's Pace.
   - High-visibility [⏳ Park in 7-Day Impulse Vault] intercept button when triggerImpulseVaultNudge == true.
2. Habit Macro Strip: Horizontal row of 1-tap AssistChip items for the user's top 5 exact frequent transactions.
3. Mode-Shift Segmented Row: Switch between [Expense], [Income], [Transfer], [Lent / Split], and [Impulse Vault].
   - When [Transfer] is active: Show Source Wallet -> Destination Wallet selector + inline +₱15 Fee quick chip.
   - When [Lent / Split] is active: Show Debtor Name input + split ratio selector (50/50, Equal Split N, or Custom).
4. Chrono-Contextual Category Pills (Swipe-Up to Commit):
   - Horizontal row of predicted categories above the keypad.
   - Attach Modifier.pointerInput with detectVerticalDragGestures: Swiping up on any Category Pill (or tapping a pill when an amount is entered) selects that category AND commits the transaction in a single gesture.
5. Custom Inline-Math Keypad & Micro-Syntax Bar:
   - 4x4 tactile grid (0-9, ., +, -, *, /, ⌫, = / Commit) plus an expandable top text bar for batch Micro-Syntax entry.
1. Hero "Safe-to-Spend Today" Ring: Custom Compose Canvas circular progress gauge. Tapping the center toggles between PHP Currency (₱300.00) and Real-Life Meal Units (4.0 Meals).
2. Smart Action Inbox: Collapsible top card list displaying pending lock-screen notification captures, unsettled [Lent / Split] IOUs with a 1-tap [Mark Paid] button, and Impulse Vault items whose 7-day timer has unlocked.
3. Velocity Budget Bars: List of categories where each progress bar renders:
   - Colored fill for current spend.
   - A sharp vertical Pace Tick Mark representing dayOfMonth / daysInMonth.
   - A 6-month trend sparkline drawn via Compose Canvas.
Build all charts natively using androidx.compose.foundation.Canvas (zero third-party chart libraries):
1. Macro Cash-Flow Sankey Diagram: Draws smooth cubic Bézier ribbons (Path.cubicTo) flowing from Income -> Wallets -> Expense Categories & Net Savings.
2. Micro-Leak Radar & Temporal Trigger Card: Highlights the user's highest-burn 3-hour time window and lists cumulative totals of < ₱150 transactions.
3. Burn-Rate Velocity Curve & GitHub-Style No-Spend Heatmap:
   - Cumulative line chart plotting This Month's Spend (solid line) vs. 3-Month Average (dashed line).
   - 7x5 monthly heatmap grid shading days from emerald green (₱0 discretionary No-Spend Day) to deep coral.
4. "Ghost Budget" Sandbox Mode Toggle:
   - Toggling Sandbox Mode turns category bars into interactive Slider controls.
   - Dragging any slider recalculates in-memory (Dispatchers.Default, without mutating SQLite) how many days/weeks earlier the user will reach their savingsGoalCentavos and animates the projected completion date.
1. Multi-Wallet Carousel: Displays Cash, GCash/Maya, and Bank balances with net liquid total.
2. Impulse Buy Purgatory List: Shows items currently in their 7-day cooling-off period with a live countdown progress bar. Once unlocked, displays two buttons:
   - [🎉 Skip It & Sweep to Savings] — Moves the item's amount into lockedSavingsCentavos and increments the lifetime "Money Saved from Impulse Buys" trophy counter.
   - [Buy Anyway] — Converts the item into a standard expense.
1. No Placeholders or TODO() Blocks: Write complete, compilable Kotlin code for every file, including the SQLite DAO queries, InlineMathEvaluator, MicroSyntaxLexer, EvaluateKeypadImpactUseCase, and Compose Canvas drawing math.
2. Pre-Populated Demo Seed Data: In KipitDatabase.Callback, seed Default Wallets (Physical Cash, GCash, Maya, BPI Savings), Default Categories (Food, Transport, Utilities, Tech & Gaming, Groceries, Subscriptions), and 30 days of realistic sample transactions so all Chrono-Contextual predictions, Sankey ribbons, heatmaps, and micro-leak analytics render immediately on first launch.
3. Step-by-Step Output Execution:
   - Step 1: Output build.gradle.kts (App module) + Centavos value class + all Room Entities, TypeConverters, KipitDao, and KipitDatabase.
   - Step 2: Output the Domain Use Cases (EvaluateKeypadImpactUseCase, CalculateSafeToSpendUseCase, InlineMathEvaluator, MicroSyntaxLexer) + KipitRepositoryImpl.
   - Step 3: Output the MVI ViewModels and Jetpack Compose UI for OmniSheetModal, PulseDashboardScreen, DeepLabAnalyticsScreen (including custom Canvas Sankey & Heatmap code), and VaultScreen.
   - Step 4: Output EWalletNotificationListener, SurplusSweepWorker, KipitGlanceWidget, QuickMacroTileService, and AndroidManifest.xml.
Begin immediately with Step 1.
```

---

### How to Get the Best Output from This Prompt
* **In an Agentic IDE (Cursor Composer, Windsurf, Claude Code, or Android Studio Agent Mode):** Paste the entire prompt at once. Because Section 8 defines a strict **4-Step Execution Order**, the agent will build the project file-by-file from the data layer up without hitting token output truncation.
* **In a Standard Chat Window:** Paste the prompt as-is; when it finishes generating **Step 1** (Gradle + Room SQLite Schema & Queries), simply reply `"Continue with Step 2"` until all four layers are complete.

<FollowUp label="Want me to generate Step 1 (Gradle, Centavos class, and full Room SQLite
```