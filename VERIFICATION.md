# Verification — 29 October 2026

Command: `gradlew.bat assembleDebug assembleDemo testDebugUnitTest lintDebug`
Result: BUILD SUCCESSFUL. Both APKs produced; 14 JVM tests passed with zero failures/errors. Lint has zero errors; dependency-update and target-version warnings remain. The project uses AGP 9.1.1, Gradle 9.3.1, Kotlin 2.2.21 and compile SDK 37 (target SDK 36).

Eight money tests cover precedence, incomplete operators, exact decimal arithmetic, zero division, overflow, month end, reserved savings and split rounding. Six notification tests cover outgoing payment extraction, package spoofing, incoming payments, OTPs, failed payments and fee separation. These are not database transaction integration tests.

APK permission inspection confirms no INTERNET, camera, or gallery permission. WorkManager adds network-state inspection, boot, wake-lock and foreground-service permissions. Native backup and device-transfer exclusions are configured. Mirrored Kipit design skills are byte-identical.

Device launch and interaction verification is pending. No performance, 120 FPS, sub-millisecond query, external provider template, release-signing, or store-readiness claim is made.
## Improvement increment verification
The final command `gradlew.bat assembleDebug assembleDemo testDebugUnitTest lintDebug` completed successfully in 9m 15s (36 tasks executed, 66 up-to-date). All 25 JVM tests passed: 8 existing money tests, 6 parser tests and 11 new preview/rounding/overflow tests. Lint has no errors; warnings remain. An earlier build overlapped DAO edits and failed with stale generated DAO code; the final rerun regenerated code and passed.
The freshly built demo APK was installed over the existing demo only. Its dashboard and entry sheet launched on the emulator; screenshots and UI hierarchies are in the chat visualization folder. No financial payment was committed in this smoke check. This does not verify process recreation, database rollback/concurrency, migration, provider notification formats, complete accessibility, or physical-device performance. The full improvement plan remains incomplete as listed in IMPLEMENTATION.md.

Emulator UI hierarchy confirms Food displays “₱2,460.00 over budget” and the initial sheet exposes Record at bounds [428,2242][652,2295]. Tapping the Commute repeat chip filled a ₱30 draft and kept the sheet open; Record was not pressed.

## Approved UI redesign — final verification

## Final APK verification

Final command: `gradlew.bat assembleDebug assembleDemo testDebugUnitTest lintDebug --max-workers=1 --no-daemon`. BUILD SUCCESSFUL in 7m 54s, 26 tasks executed and 76 up-to-date. All 25 unit tests passed with zero failures/errors. Lint has zero errors, 12 warnings and 1 hint. Existing demo-seed type inference and deprecated build-option warnings remain; no UI icon deprecation warning remains after using the mirrored icons.

The final demo APK was installed and launched. A repeated rejected ₱6,000 payment retained its amount and Cash balance; the error is now visible immediately under the amount with the keyboard open (`final-entry-error-keyboard.png`). Final light screenshots confirm the compact Activity filters/rows and right-aligned wallet balances. Final dark captures cover Today, entry, Activity, Wallets, budgets, the budget editor, Settings and Insights. Default-size primary views were visually inspected, as were the earlier 200%/compact views. Night mode, size and text scale were restored to the original settings (no / physical 1080×2400 / 1.0).

Main APK SHA-256: 6AD409C5647071FD3830EE3213D98DA661303866A223B64A05E9ABC33D51121B
Demo APK SHA-256: 9222C9DD33AA98CF753C183B999DABBDB8D36C4F0FF84D0EB0EA40F16A89AB41

Both Kipit design skill mirrors are byte-identical. No physical-device, complete TalkBack or performance certification is implied by this result.

## Side-by-side money directions — 29 October 2026
Today and Wallets now show Money in (Income / Credit) beside Money out (Expense / Debit). Each opens the matching existing form directly, with no Other transaction step. The controls have equal width, direction icons and visible surfaces; at 200% system font they stack and remain readable.

Final command: `gradlew.bat assembleDebug assembleDemo testDebugUnitTest lintDebug --max-workers=1 --no-daemon`. BUILD SUCCESSFUL in 5m 15s, 27 tasks executed / 75 up-to-date. Current XML reports contain 30 passing tests, zero failures/errors; test results were reused. Lint has zero errors, 13 warnings and 1 hint. An earlier lint attempt reported PropertyEscape for a local Java path. That setting was absent from the current gradle.properties when inspected; rerunning the full command against the current configuration passed. No build/signing setting was edited for this UI request.

Runtime checks used only com.kipit.app.demo on the Android emulator. The two controls were visibly side by side on Today and Wallets. Money in opened Add income/To wallet; saving sample income of PHP100 raised Cash from PHP4,980 to PHP5,080 without increasing spending (PHP270). Money out opened Add expense/From wallet; saving PHP10 lowered Cash to PHP5,070 and increased today's spending to PHP280. Available today finished at PHP7,930, reflecting the net PHP90 over three remaining days. Large-font layout was checked and the original 1.0 scale restored. The latest demo APK was reinstalled after the final build; personal ledger data was not opened or modified.

Screenshots and hierarchies: flow-buttons-today, flow-buttons-wallets, flow-income-form, flow-expense-form, flow-after-income, flow-after-expense-wallets, flow-after-both, flow-buttons-large, flow-buttons-latest in the chat visualization folder. Both Kipit design skill mirrors were synchronized. Physical-device and full TalkBack validation remain outside these emulator checks.
