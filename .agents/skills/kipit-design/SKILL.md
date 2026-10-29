---
name: kipit-design
description: Maintain the native Kipit budget app's design, financial UX, and truthful state boundaries.
---
# Kipit design and interaction reference
Read design-system/MASTER.md and IMPLEMENTATION.md before changing screens. The parent repository's ui-ux-pro-max skill provides design guidance; its marketing-page recommendations do not override this native Android brief.

Keep money in exact Long centavos. Float is for visual ratios only. Show transaction failures and preserve entry values on failure. A displayed success requires a committed Room transaction. Keep personal and sample app data isolated and label sample screens clearly. Captured notifications are proposals until reviewed. No clinical/MedPulsePH assumptions belong in this app.

Use Material semantics, visible field labels, system light/dark themes, large touch targets, legible currency, and textual chart summaries. Update this skill and its .claude mirror alongside design-system/MASTER.md when interaction or visual rules change. Record device testing separately from compilation.
Entry revisions: restore draft primitives with rememberSaveable and resolve captured payments by saved ID. Repeat chips fill a review draft. Record remains outside the scrolling form. Use OPENING for existing money; do not count it as income. Category budgets are editable. Preview rolling allowance by projecting wallet effects and bill payments. Sandbox percentages round HALF_UP to centavos and make no completion-date promise from partial-month data.

Approved clarity redesign: prioritize Available today; navigation is Today/Activity/Wallets/Insights with Settings separate. Use shared UiComponents controls, icon-and-label financial actions, one-at-a-time budget/preference editors, saved tab state, and bounded home summaries. Keep expense entry focused; show captured payments for review, optional tools deliberately, parsed batch entries before save and errors inline. Do not restore the oversized ring, decorative charts, unobserved no-spend claims or invented historical averages.

Routine entry update: Today and Wallets show peer Money in (Income / Credit) and Money out (Expense / Debit) controls side by side. Each opens the matching form directly. At enlarged font scales they stack for readability. Credit/debit here describe wallet flow, not debt-account support.
