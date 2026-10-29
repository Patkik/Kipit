# Kipit design system

User-approved 29 October 2026. Canonical profile: .impeccable.md. Layout: UX_REDESIGN_BRIEF.md.

Audience: personal spenders, students and households. Primary information is how much can be spent today. Tone is calm, practical and clear. Household audience does not imply shared-account functionality.

Navigation: Today, Activity, Wallets and Insights. Settings is a separate toolbar destination. At expanded width (600dp+) use a navigation rail and constrain page content to 720dp. On phones use four labeled bottom destinations.

Today: left-aligned allowance without a decorative ring, calculation detail, spent/reserved totals, paired Money in / Money out actions, bounded review/budget/recent summaries. Full history is in Activity. Budgets edit one item on demand. Wallets contains balances and entry points to Savings, Planned purchases and Budgets. Preferences and permissions live in Settings.

Shared primitives in presentation/UiComponents.kt: AppPage, SectionTitle, ActionButton, ActionRow, MoneyRow, LedgerRow, DetailSheet, EmptyState and ErrorNotice. They use Material semantics, system touch feedback, explicit control boundaries and icon-plus-label actions. Do not use plain text links for recording money, settlement or saving budgets. Do not create identical decorative card grids or nested cards.

Spacing scale: 4/8/12/16/24dp, page margin 20dp. Shared ActionRow padding is 16dp with 12dp icon/text gaps; forms and sheets group tasks with 16–24dp separation. Action buttons use 48dp minimum secondary / 52dp primary heights. Corners are 12–14dp, without decorative shadows.

Typography uses bundled system sans. Five roles: large tabular allowance, screen title, section/money title, body/control label, metadata. Amounts and transaction rows use tabular figures. At font scale above 1.3, amounts and filters stack rather than squeeze columns. Labels and amounts can wrap; no fixed-height text containers.

Palette is defined completely in Theme.kt: forest primary, warm forest-tinted paper/mist surfaces, coral error and slate tertiary. All important Material surface/container/outline roles are set in light and dark themes to prevent default lavender. State is expressed with words as well as color.

Routine entry: Today and Wallets show paired Money in (Income / Credit) and Money out (Expense / Debit) controls, opening the matching forms directly. Keep both side by side at standard text sizes and stack at enlarged fonts.

Entry: amount, selected category, selected wallet and fixed save footer. Notes, calculator, repeats and batch entry open deliberately. Other transaction is a separate chooser. Captured payments stay in review mode. The preview uses exact projected wallet effects; saved drafts keep a stable operation key. Errors stay visible in the sheet and are announced politely. A success means the Room write committed.

Insights: selected-period recorded category spending first; detailed patterns, time windows, daily history and hypothetical scenarios open separately. No invented three-month average or confirmed no-spend claims for missing days. Chart values have readable rows. Opening money is not earned income; reservation is not cash creation.

Motion: use native Material sheet, toggle, focus and ripple feedback; no entrance choreography, bounce, confetti or animated monetary values. Native animation settings apply. A calm financial app does not need every available visual effect.

Accounting, offline scope, sample isolation and permissions remain governed by AGENTS.md. Changes to design/interaction require this document and both kipit-design skill mirrors to be updated together. Runtime and accessibility claims require evidence in VERIFICATION.md.
