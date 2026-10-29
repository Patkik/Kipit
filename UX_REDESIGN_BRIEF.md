# Kipit clarity redesign — profile and proposed brief

29 October 2026. Profile and layout explicitly approved by user: “Yes — build this layout.” This document supersedes earlier proposed UI priorities: the first question is how much can be spent today, with expense entry supporting that task.

## Confirmed profile

Audience: personal spenders, students and households. Tone: calm and practical. Visual language: crisp controls, clearly separated sections, restrained forest accents. Primary goal: see how much can be spent today. Explicit anti-goals: walls of text, weak text-only actions, confusing feature mixtures and generic AI styling.

The audience includes households; shared ledgers, accounts or synchronization are not implied or implemented.

## Observed causes of overload

Evidence is the current Compose source and the emulator inspection from the preceding implementation turn, rather than a fresh usability study.

| Surface | Current problem | Effect |
|---|---|---|
| Home/Pulse | 248dp allowance panel, motivational heading, spending totals, category explanations and every transaction in one list | The first answer is visible, but scanning beyond it becomes an unbounded task. |
| Entry | Six horizontal modes, note field, wallet/category chips, repeats and advanced options | Users must distinguish several different jobs before completing one simple expense. |
| Wallets/Vault | Balances, savings, impulse purchases, six editable budgets, preferences and permission controls | Everyday review and occasional configuration have no clear boundary. |
| Insights/Deep Lab | Six section-heading call sites and several explanations, charts, sandbox and search | No concise overview or clear path to a specific question. |
| Actions | Settlement, capture review, impulse decisions and budget saves use TextButton controls | Important actions resemble surrounding copy and hierarchy is weak. |
| Language | Pulse, Deep Lab, Ghost budget and Savings lock require interpretation | Navigation and financial meaning depend on remembering product terminology. |

Static counts are diagnostic only: Entry contains 28 Text call sites; Wallets contains 17. Loops, conditional states and SectionTitle-generated text mean these are not visible-element counts or word counts. No usability score is asserted.

## Proposed feature summary

Reorganize the existing offline Android app around today's spendable amount and simple task destinations. Preserve features and financial rules while reducing the number of simultaneous decisions. This is a structural redesign, not a request to turn every element into a card.

## Primary action and information hierarchy

The primary information is **Available today** with its exact peso amount. The primary action is **Add expense**. The amount gets typographic emphasis without an oversized decorative ring. A clearly bounded “How it works” detail opens the calculation explanation.

## Proposed navigation and layout

Bottom navigation: **Today · Activity · Wallets · Insights**. Settings is reached from a labeled, accessible toolbar control. Expense entry has a clearly styled primary button.

### Today

- Compact title/date.
- Available today, short status and calculation-detail entry point.
- Two subordinate values: Spent today and Reserved savings.
- Add expense as the single strongest action.
- A review row with count when pending items exist, opening a focused review list.
- At most three category budget summaries with “All budgets”.
- At most three recent transactions with “All activity”.

Full history, preference fields, permission explanations and scenario sliders do not live here. If allowance is negative, the screen states the deficit without reassuring green styling.

### Add expense

- A focused entry surface with a short title and clear close/back control.
- Amount is the first field.
- Category and wallet are bounded selector rows showing the current selection and a chevron.
- Note is optional behind “Add note”.
- One concise projected-allowance line; fees and split cash effects remain visible when relevant.
- Fixed Save expense button with icon, loading state and explicit disabled/error states.
- “Other transaction” opens choices for income, transfer, split, opening balance and parked purchases. These do not compete across a permanent six-chip strip.
- Calculator, repeat expenses and batch entry have clear secondary entry points. Batch remains a distinct task, not an extra form below the expense form.

### Activity

- Search and compact filter controls.
- Date-grouped transaction rows with type/category icon, short title, wallet/date context and aligned amount.
- Clearly differentiated expense, income, opening and transfer labels.
- Honest scope: entry details/corrections must not be advertised as working until their underlying implementation exists.

### Wallets

- Readable wallet rows with balances, avoiding a carousel that conceals other wallets.
- Separate Savings and Planned purchases entry points.
- Budgets reached through “Manage budgets”; show summaries first, edit one item on demand rather than six persistent text fields.
- Preferences and notification permissions move to Settings.

### Insights

- A clear period selector and one spending summary.
- Category breakdown first.
- Separate detail views/expanders for recurring expenses, spending times and history.
- Scenario exploration is labeled “Try a budget change”, explicitly hypothetical.
- Data coverage and unknown states remain truthful; shorter text must not remove essential calculation caveats.

### Settings

- Grouped rows: Budget preferences, Savings goal, Payment capture, Reminders, About/data storage.
- Open each editor deliberately. No long embedded form as the default view.
- Show permission status, then a bounded action to change it.
- Backup/restore is not displayed as available until implemented.

## Button and component system

Primary financial action: solid forest surface, icon plus short label, 52dp preferred height. Secondary action: outlined or tonal surface, icon plus label. Navigation/action row: bounded touch area, leading icon, label/value and trailing chevron. Tiny text links are reserved for low-priority help, not recording/settling money.

Use one coherent native icon family and consistent stroke/size. Standard Android ripples, focus and selected states remain. Important actions have visible loading/disabled/error feedback. Icons aid recognition; labels remain necessary. Do not give every action the same filled style.

## Spacing and typography

Proposed phone scale: 4/8/12/16/24/32dp. Use 16–24dp page margins, 8dp between label/value or icon/label, 12–16dp within a task group and 24–32dp between distinct sections. Minimum 48dp touch targets. These are native phone values, not copied web whitespace rules.

Use a small, explicit text hierarchy: prominent allowance, section title, body/control label, secondary metadata. Keep monetary figures tabular and consistently aligned. Do not shrink text to fit an overloaded screen. Reflow at larger font scales. Preserve bundled system typography under the project's native design rules.

## Content requirements

Replace motivational headings with functional labels. Examples: “A little room to breathe” → “Today”; “Needs a moment” → “To review”; “Your spending pace” → “Budgets”; “Ghost budget” → “Try a budget change”. Replace generic “Paid” with “Record repayment” where that is the actual action.

Default helper copy is one short line where needed. Detailed accounting explanations are available on demand. Required financial consequences, errors, fees and permission scope stay visible in the relevant flow.

## Key states

- First use: one opening-balance setup action, no simultaneous tutorial paragraphs.
- No entries: short empty message and Add expense.
- Pending review: count and clear review destination; no automatic commitment.
- Saving: relevant action shows progress; preserve draft on failure.
- Validation error: local field message explaining the fix.
- Insufficient funds: show selected wallet and shortfall; keep input intact.
- Negative allowance: explicit deficit and access to calculation details.
- Missing insight history: unavailable comparison, never an invented average.
- Long labels/large amounts/200% font: wrap or reflow with all required controls reachable.
- Permissions denied: manual use remains available and status is clear.

## Impeccable implementation passes

1. Shape and teach: this profile and brief.
2. Distill, clarify and arrange: information architecture, concise copy, meaningful grouping.
3. Typeset, normalize, colorize/quieter: consistent typography and semantic contrast appropriate to the chosen calm direction.
4. Extract: shared buttons, selection rows, section containers, transaction rows and spacing tokens.
5. Onboard: concise opening-balance and empty-state flow.
6. Adapt and harden: compact widths, keyboard, large fonts, long data and failure states.
7. Animate and delight: only purposeful disclosure/feedback after hierarchy is correct; honor system motion settings.
8. Optimize: avoid adding expensive rendering work or repeated history scans.
9. Critique and audit: independent design assessment, code checks and actual native screenshots. Web-only detectors must not be presented as Compose evidence.
10. Polish: final alignment, control states and visual consistency after functional verification.

Bolder and overdrive are evaluated against the profile; visually aggressive effects are not automatically added to a calm finance app. The full toolkit informs the review without forcing contradictory visual treatments into the UI.

## Verification and acceptance

- Within a brief glance, a reviewer can identify today's amount and Add expense.
- Today shows bounded summaries; every hidden feature has a clear destination.
- No required action is styled as a plain paragraph.
- No category budget input appears until editing is requested.
- Save expense remains reachable with the keyboard visible.
- Screenshots are reviewed for Today, entry, Activity, Wallets, budget editor, Settings and Insights in both themes.
- Verify compact width, large font, empty/error/pending data and populated demo states on the emulator; distinguish physical-device testing still outstanding.
- Run assembleDebug, assembleDemo, testDebugUnitTest and lintDebug. Preserve all money tests and offline permissions.

## Approval recorded

The user approved Today, Activity, Wallets, Insights; Settings separately; one focused Add expense flow. The audience, tone and primary goal are confirmed. No further confirmation is required for this redesign.

Skill reference for the brief-confirmation step: C:/Users/patri/.agents/skills/shape/SKILL.md — “Get explicit confirmation of the brief before finishing.”
