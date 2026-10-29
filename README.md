# Kipit
Native Android offline-first behavioral budget tracker. Kotlin, Jetpack Compose, Room/WAL, Hilt/KSP, WorkManager, Navigation 3 and Glance. No INTERNET, camera, gallery, OCR or analytics permissions.

Open this folder in Android Studio. JDK 21 and Android SDK 37 are required. Run `gradlew.bat assembleDebug testDebugUnitTest lintDebug`. Install `app/build/outputs/apk/debug/app-debug.apk` on Android 8+.

## Money and accounting
All ledger amounts use signed Long centavos and exact arithmetic. Decimal input and expression arithmetic use BigDecimal; Float is limited to presentation ratios and charts. Splits debit the full payment but record only the personal share as category spending; repayment restores cash without counting as income. Receivables are NOT subtracted a second time from already-debited wallet cash. Transfers are atomic and debit fees separately. Savings reservations never create cash. Batch entries are all-or-nothing.

## Scope and current limitations
Today, Activity, Wallets, Insights and Settings use real local Room data. Initial wallets are zero and categories are provided; no synthetic transaction is silently inserted into a user's real ledger. A separate `.demo` application build contains 30 days of sample expenses in an isolated database and displays a Sample history label. Run `gradlew.bat assembleDemo` for that APK. Notification parsing is conservative and captures await confirmation; templates require device testing against real provider samples. Notification access and reminder permission are optional and user-controlled. Raw notification text is not persisted. Quick Settings opens the app; it does not spend money from a locked device. The widget shows safe-to-spend and opens the app. Workers schedule approximately midnight/21:00, subject to Android power management, not exact alarms.

This initial version does not yet implement swipe-to-commit gestures, notification category action buttons, one-tap widget/tile macros, category-level sandbox sliders, a dedicated MVI ViewModel for each screen, historical daily budget snapshots, a durable per-day streak reconciliation ledger, data export/restore, encrypted database, or a sample-data selector. The chart is an aggregate cash-flow sketch rather than a fully normalized wallet-to-category Sankey. No rendering-speed or SQL-latency claims have been verified. These are tracked in IMPLEMENTATION.md. Do not describe those as implemented.

## Design references
Uses the parent repository's `.claude/skills/ui-ux-pro-max/SKILL.md`, its Jetpack Compose search guidance, and the MedPulsePH frontend-design skill as references. Design decisions are in `design-system/MASTER.md`.

## Daily entry
On Today or Wallets, choose **Money in — Income / Credit** for received money, or **Money out — Expense / Debit** for spending. Each button opens its form directly. Choose the wallet, enter the amount and save. Use Other transaction for transfers between your own wallets, split payments, planned purchases and opening balances.
