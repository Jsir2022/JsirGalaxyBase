# Standardized Market UX Completion v1

## Goal

Complete the standardized market as a trustworthy, readable trading workflow rather than a collection of technically connected controls. The server remains authoritative for price, fee, balance, inventory, order, and delivery decisions.

## Completion Matrix

## Implementation Status

Implemented in the 2026-08-15 completion pass:

- The order ticket now offers `25% / 50% / MAX` quantity shortcuts and `BID / LAST / ASK` price shortcuts, while showing the account source or destination, estimated gross, server-recalculated fee policy, and a concrete disabled reason.
- Live refresh exposes `fresh / refreshing / delayed / stale` states without overwriting an active editor or modal flow.
- Personal history rows show localized product names, fill percentage, lifecycle state, precise time, and cancellation only while an open remainder exists.
- Localized browser text can resolve to a stable server product key without replacing the text visible to the player.
- Browser empty states distinguish stale data, an unmatched search, an unmatched filter, and an empty formal catalog.
- Important history and refresh states now pair color with explicit text and numeric progress.

Visual acceptance correction after the first live review:

- The detail account card no longer embeds a full action-response sentence. It now exposes only a compact synchronization state and pending-delivery count; the complete response remains in the notification layer and order history.
- The buy/sell popup is grouped into quote context, order parameters, settlement preview, and final actions. Quantity and price shortcuts stay close to the field they modify, while the server-authoritative validation reason remains visible above confirmation.
- The personal order center uses a compact left-aligned filter toolbar instead of four full-width option blocks. Active constraints are summarized beside the toolbar, and reset is always available without consuming table width.

These corrections define visual completion as a visible hierarchy change in the live terminal, not merely the presence of additional fields or passing tests.

Order and asset center closeout (2026-08-16):

- The detail footer is now fixed to `买入 / 卖出 / 撤单`. `撤单` opens a current-product selector containing only the authenticated player's cancellable remainder, followed by a version-bound confirmation with the exact return amount and destination.
- The old embedded history entry is replaced by the full-screen `订单与资产中心`, with six account summary cards, four fixed tabs, a bounded filter toolbar, a server-paged table viewport and fixed footer. Mouse-wheel paging is accepted only over the table.
- `当前委托`, `成交记录`, `资产与交付`, and `历史查询` are backed by typed server snapshots. The wire rows carry stable registry/meta item identity and numeric order fields; the client creates the real `ItemStack` and localizes its name without splitting display text.
- Asset exceptions combine pending deliveries, market recovery/failure logs, Base Vault `FAILED` / `RECOVERY_REQUIRED` operations, and a full-Vault marker when claims cannot be delivered. Recovery remains manual and audited.
- Market and Vault status-band icons route directly to the appropriate tab. Action Toasts are clickable inside the terminal and focus the relevant order, trade, custody, or recovery record.
- Pagination uses repository counts, including the required `11 records / 4 per page = 3 pages` boundary. Search, enum, page index, page size and focus identifiers are bounded server-side, and account ownership always comes from the logged-in server player.

The following remain manual visual acceptance items rather than additional backend work: supported GUI-scale fit, keyboard and mouse ergonomics, top-right notification readability, chart crosshair readability, and the clarity of each empty or stale state in the real GTNH font.

### 1. Order ticket

- Existing: buy/sell side, market/limit type, quantity, limit price, maximum quantity, confirmation popup.
- Complete when: quantity offers 25%, 50%, and maximum shortcuts; limit price can use bid, last, or ask; source and destination are explicit; estimated gross and fee policy are visible; disabled submit has a concrete reason.
- Server rule: the preview never authorizes settlement. The server recalculates price, fee, balance, and inventory.

### 2. Freshness and quote protection

- Existing: bounded five-second live refresh, single in-flight request, modal/input protection.
- Complete when: the UI distinguishes fresh, refreshing, delayed, and stale data; a stale quote is never presented as current; editing fields are not overwritten; material price movement requires a new confirmation.

### 3. Personal order center

- Existing: product search, product/side/status/time filters, reset, server pagination, per-order cancellation.
- Complete when: rows use localized product names, direction and status labels, fill progress, precise time, and cancel only for open remainder; filters always have an obvious reset path and pagination accounts for every row.

### 4. Actionable notifications

- Existing: global top-right notifications for terminal market feedback.
- Complete when: success, warning, recovery, and failure use distinct titles and concise causes; routine refresh never emits a notification; relevant feedback identifies the affected product/order and tells the player the next action.

### 5. Localized discovery

- Existing: visible product names resolve from the real `ItemStack` and current client language.
- Complete when: search also matches the localized display name without weakening server-side product-key validation. Database aliases remain fallback terms, not the displayed source of truth.

### 6. Professional chart interaction

- Existing: OHLC/price-line downgrade, continuous time buckets, volume, crosshair and OHLCV readout.
- Complete when: current price is visually marked; real-trade and carry-forward buckets are distinguishable; sparse and empty states are explicit; axes and readout remain inside the chart at supported GUI scales.

### 7. Loading, empty, stale, and error states

- Complete when the market distinguishes: loading, no catalog matches, no trades, no order-book liquidity, database unavailable, permission denied, stale cached data, and retryable network delay. These states must not collapse into an empty dark panel.

### 8. Status beyond color

- Existing: BUY/SELL labels and numeric signs in several views.
- Complete when every important green/red state also has text, sign, arrow, or shape. Color may reinforce meaning but must not be the only carrier.

## Implementation Order

1. Order ticket and server-confirmation wording.
2. Freshness state and non-noisy feedback.
3. Order center row clarity and actionable notifications.
4. Localized search bridge.
5. Chart annotations and explicit loading/empty/error states.
6. Non-color-only review across browser, hover, detail, and history.

## Verification

- Static: `git diff --check`.
- Targeted tests: order ticket, live refresh, market visuals, screen state, history paging, snapshot and action payload round trips.
- Deployment: Lobby, S2, and client with artifact hash verification.
- Manual acceptance: order ticket keyboard/mouse flow, stale refresh behavior, localized search, history filters/cancel, top-right feedback, chart crosshair, and error/empty states.

## Boundaries

- Do not reintroduce player-hand or player-inventory trading. Personal Base Vault is the current account inventory.
- Do not fabricate market prices, trades, candles, volume, or liquidity.
- Do not rewrite matching, bank settlement, escrow, claim, or recovery services for visual work.
- Custom and exchange markets keep their own business semantics; shared UX components may be reused only where the meaning remains correct.
