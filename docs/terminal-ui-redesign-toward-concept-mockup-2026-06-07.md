# Terminal UI Redesign Toward Concept Mockup

Date: 2026-06-07

This document records the product-level redesign direction for the `JsirGalaxyBase` terminal UI after the first real ServerTools cross-server warp validation succeeded in game.

The current conclusion is:

- transport backend validation is no longer the blocker for the group-server page
- the main blocker has shifted to terminal usability and visual structure
- the current BetterQuesting-style in-game route remains the correct long-term technical direction
- the current terminal implementation is functionally ahead of its interaction quality

This document is the overall redesign plan for moving the current terminal closer to the intended in-game concept quality.

## Current Confirmed State

Confirmed in game:

- `Lobby <-> S2` warp transfer is working
- terminal can open through the formal player entry
- current terminal already contains the `传送 / 群组服` page and real warp action chain

Confirmed problem:

- the current UI is still too dense in the wrong places
- navigation, top bar, and content hierarchy are harder to use than they should be
- the current page structure still feels like migrated technical panels rather than a clean in-game console
- the current `传送 / 群组服` page is present, but its running layout still does not match the target concept shape

So the redesign scope should now focus on:

1. shell usability
2. page hierarchy
3. transfer-page workflow clarity

It should not reopen the backend warp implementation unless a new defect appears.

## 2026-06-07 Concept Comparison Findings

After comparing the current running terminal against the target concept image, the main conclusion is:

- the blocker is no longer “whether transport exists”
- the blocker is now “the current page model is wrong for the intended workflow”

This matters because the current issue is not a small hover bug or a single spacing defect. The current `传送` page is still structurally built like a migrated generic terminal section, while the target concept is a purpose-built transport tool page.

### What The Target Concept Actually Is

The target concept is a fixed workflow-oriented shell with:

1. a narrow permanent left navigation rail
2. a middle warp list region
3. a right detail and action region
4. a compact top bar with breadcrumb, contribution, and small utility actions
5. one dominant primary action: `确认传送`

This means the player should understand the page by scanning columns, not by reading a long mixed content block.

### Where The Current Running Page Still Differs

#### 1. Information architecture is still wrong

Target concept:

- left navigation
- middle selectable warp list
- right structured detail and transfer status

Current running page:

- still behaves like one large content body
- mixes server directory, warp listing, explanation, and actions in one scrolling region
- still relies on a section-style content host rather than a dedicated transport workflow layout

So the problem is not just “text overlap”; the page is built on the wrong content model.

#### 2. Navigation form is still wrong for this page

Target concept:

- always-visible narrow rail
- icon plus single-line label
- clearly secondary but always reachable

Current running shell:

- hidden-edge rail / expandable menu behavior
- more suitable for general shell experimentation than for the first core workflow page

For `传送 / 群组服`, the correct product decision is:

- do not require hover-only discovery
- allow a stable narrow navigation rail
- treat transport as a first-class tool page, not a hidden subsection

#### 3. The middle region should be a true warp list, not text rows

Target concept middle column contains:

- list title such as `可用传送点`
- capacity summary such as `2/32`
- stable row cards
- icon
- main title
- subtitle
- availability state
- obvious selected row highlight

Current running page still renders warp data too close to text-list form:

- no strong card rhythm
- no clear selected-row dominance
- no stable list item geometry
- no clean separation from footer/button regions

That is why the current page can still collapse into overlap near the bottom action area.

#### 4. The right region should be structured detail, not descriptive body text

Target concept right column is a field-driven panel:

- current server
- target server
- warp name
- transport description
- recent transfer status
- warning banner
- primary CTA

Current running page still behaves closer to a mixed description/report block:

- too much body text in one content area
- status and detail are not visually separated strongly enough
- action area does not clearly dominate the task flow

#### 5. Action priority is wrong

Target concept:

- `确认传送` is the dominant action
- refresh is a small top utility action
- explanation/help is secondary

Current running page:

- bottom `刷新` and `关闭` style controls still take too much visual weight
- the main transfer action does not yet anchor the page

The product implication is:

- transport pages should have one main CTA
- secondary actions should move into compact top utilities or low-emphasis controls

#### 6. Current page still reads like a system panel, not a player tool

Target concept language is operational:

- choose destination
- inspect current/target server
- review recent transfer result
- confirm transfer

Current running page still leans too much on:

- explanation blocks
- generic section headers
- migration-era “all information is shown somewhere” habits

The redesign must push the page from “describing the transport system” to “helping the player complete a transport action”.

#### 7. The right side should become one continuous workbench, not three equal cards

The target concept does not read like:

- one detail card
- one recent-status card
- one action card

It reads like one continuous operational surface:

- top: selected destination detail
- middle: recent transfer result
- lower: warning and confirmation

The current running implementation is already close in data shape, but still too obviously assembled from three parallel cards.

Updated product decision:

- keep the existing backend snapshot and action chain
- keep the left warp list
- rebuild the right side into one continuous workbench with:
  - field rows
  - compact recent-state panel
  - feedback strip
  - warning strip
  - one dominant confirm button

#### 8. Use icon-like visual anchors wherever possible without adding a new asset pipeline

The target concept feels better partly because:

- navigation uses icons
- warp entries use icon rhythm
- detail rows have small visual anchors
- state panels are not pure text blocks

For the current GTNH terminal framework, the near-term rule should be:

- prefer lightweight in-framework iconization first
- use small drawn glyph blocks, status dots, and icon anchors inside rows/cards
- avoid waiting on a larger texture or external UI asset pipeline before improving the page

This keeps the page visually closer to the concept while preserving the current in-game GUI architecture.

## Updated Product Decision For The Transport Page

The correct next step is **not**:

- only fixing hover behavior
- only tightening spacing
- only moving one button

The correct next step is:

- keep the real warp backend chain unchanged
- replace the current generic `SERVER_TOOLS` section presentation with a dedicated transport workflow layout
- use the target concept as the visual and structural reference

In practice, that means the first fully productized page under the new terminal should be:

- a dedicated `传送 / 群组服` tool page
- with stable left rail, middle list, right detail, compact top bar, and one dominant confirm action

## Why Not Go Back To Web

This redesign is **not** a signal to move the terminal to a pure web UI.

The current repo direction already moved away from deepening `ModularUI2`, and settled on an in-repo BetterQuesting-style `GuiScreen` framework. That direction should remain.

Reasons:

1. The product is an in-game terminal, not an external dashboard.
   - mouse behavior
   - keyboard focus
   - pause/escape behavior
   - scaling
   - packet-driven state refresh
   all need to remain game-native.

2. A pure web route would add another runtime bridge instead of reducing complexity.
   - old Minecraft client environment
   - custom browser embedding/runtime concerns
   - input and rendering bridge maintenance
   - multiplayer/game-state integration overhead

3. BetterQuesting-style shell architecture is a better match for the product shape.
   - full-screen application shell
   - left navigation
   - content host
   - popup/notification/status infrastructure
   - multiple future business pages under one consistent frame

So the correct move is:

- keep the current in-game BetterQuesting-style framework
- redesign the current shell and section interaction model
- make it look and feel closer to the concept direction without abandoning the current technical base

## Gap Between Current UI And Target Direction

The concept direction and the current running terminal are still far apart.

Current issues:

1. Navigation still consumes too much attention
   - too visible when it should be secondary
   - too heavy for frequent use
   - not yet behaving like a thin tool rail

2. Top area still wastes valuable vertical space
   - repeated page/title semantics
   - status and content headers compete with each other
   - too much shell height before the actual task area starts

3. Transfer page reads like a descriptive panel instead of an operation panel
   - too much mixed information in one column
   - warp list, server list, explanation, and action compete in the same region
   - the primary action does not dominate the page clearly enough

4. Too many text blocks are explanatory instead of operational
   - the player should immediately see:
     - where they are
     - where they can go
     - which warp is selected
     - whether it is safe/available
     - how to confirm

5. The shell still carries migration-era layout habits
   - more emphasis on “all data is visible somewhere”
   - less emphasis on “the next action is obvious”

## Redesign Goal

The target is not a flashy decorative UI.

The target is:

- game-native
- compact
- readable
- operational
- visually cleaner
- better suited for repeated use

The terminal should feel like:

- a serious in-game control console
- inspired by BetterQuesting interaction language
- but more compact and purpose-built for `JsirGalaxyBase`

## Overall Redesign Principles

1. Make the shell smaller, not louder.
2. Make the current task dominant over general description text.
3. Keep only one strong primary action per page where possible.
4. Reduce duplicated labels and repeated title bands.
5. Make top-level navigation thin, quick, and peripheral.
6. Use layout hierarchy to express meaning before relying on paragraphs.
7. Treat `传送 / 群组服` as the first page to fully productize under this redesign.

## Implementation Plan

The redesign is split into three implementation phases and one visual closeout phase.

### Phase 1 - Navigation Shell Redesign

Goal:

Make the left-side terminal navigation behave like a compact tool rail instead of a large persistent menu.

Scope:

- keep only top-level entries:
  - `首页`
  - `市场`
  - `银行`
  - `传送`
- remove category title blocks from the navigation area
- convert navigation items to single-line buttons
- allow a stable narrow permanent rail when that better matches the final shell
- reduce navigation width
- ensure navigation does not steal too much body space

Acceptance:

- navigation is narrow and visually secondary
- top-level switching is immediate and readable
- navigation does not occupy a large visual column
- top-level switching remains clear
- 1280x720 still leaves enough room for the body content

### Phase 2 - Top Bar And Global Status Redesign

Goal:

Turn the top area into a true single-line status bar.

Scope:

- compress the top shell into a single row
- keep only:
  - current path / breadcrumb
  - current server or location summary
  - contribution value
  - a very small set of global actions
- remove repeated title bands from the page body
- reduce padding and border waste in the top area
- make the body begin higher on the screen

Acceptance:

- top bar height is clearly reduced
- page context is still understandable
- content starts earlier vertically
- body pages no longer repeat the same title semantics unnecessarily

### Phase 3 - Transfer Page Product Redesign

Goal:

Make `传送 / 群组服` the first truly workflow-first page in the new shell.

Scope:

- restructure the page into clear operational regions:
  - left navigation rail
  - middle warp list
  - right selected warp detail
  - recent transfer state
  - primary action
- remove or sharply reduce descriptive filler text
- make `Lobby -> S2` style state easy to read at a glance
- make the selected warp and target server obvious
- make `确认传送` the clear dominant action
- keep confirmation popup behavior
- keep the existing real backend chain untouched

Acceptance:

- a player can understand the next step without reading long paragraphs
- selecting `s2test` or `lobbytest` is visually obvious
- confirm action remains visible without awkward scrolling
- recent transfer state is compact but useful
- the page feels like a tool, not a report

### Phase 4 - Visual Closeout By Real Use

Goal:

Use actual in-game browsing to close the remaining gaps after structure is fixed.

Scope:

- text clipping fixes
- spacing and padding corrections
- button sizing corrections
- scroll viewport cleanup
- list density adjustments
- edge-reveal behavior tuning
- localized cleanup for market/bank pages if the shell redesign causes visual regressions

Out of scope:

- no new transport features
- no expansion into admin tooling
- no backend protocol redesign
- no broad market feature expansion in this closeout round

Acceptance:

- no overlapping text
- no hidden primary actions
- no broken scroll regions
- no obvious page regressions in `市场` or `银行`
- shell feels intentionally compact rather than accidentally cramped

## Recommended Execution Order

Work should proceed in this order:

1. `Phase 1 - Navigation Shell Redesign`
2. `Phase 2 - Top Bar And Global Status Redesign`
3. `Phase 3 - Transfer Page Product Redesign`
4. `Phase 4 - Visual Closeout By Real Use`

Reason:

- if navigation and top bar are not simplified first, transfer-page cleanup will keep fighting inherited shell waste
- once the shell is smaller, the transfer page can be redesigned with realistic space constraints
- only after both are stable should real-use visual cleanup begin

## Validation Strategy

Each implementation phase should be validated at three levels:

1. Static review
   - code path scope
   - no backend drift
   - no unrelated feature churn

2. Targeted test/build validation
   - compile
   - targeted terminal tests when touched
   - `git diff --check`

3. In-game visual validation
   - `25566` entry path
   - terminal open with `G`
   - `传送 / 群组服` page browse
   - `Lobby -> S2 -> Lobby` transfer confirmation chain

## Product Boundary For This Redesign

This redesign should be treated as a shell-and-workflow refactor, not a product expansion.

Do:

- improve terminal usability
- improve hierarchy
- improve transfer-page clarity
- preserve the proven transport backend chain

Do not:

- replace the framework with web
- reopen the decision to use BetterQuesting-style in-game GUI
- expand this round into full admin server management
- rework market business rules during the shell redesign

## Final Status

As of 2026-06-07:

- backend cross-server warp is proven
- current UI is functionally valid but interaction-heavy
- the terminal should now move into structured UI redesign

The next execution prompt should start from `Phase 1 - Navigation Shell Redesign`.
