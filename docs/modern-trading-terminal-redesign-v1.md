# Modern Trading Terminal Redesign v1

## Visual acceptance baseline

The latest visual target is `/home/u24/图片/市场第三版.png`. The current
implementation-to-target gap, required data fields, priority order, and
screen-by-screen acceptance checklist are recorded in
`modern-trading-terminal-visual-gap-baseline-2026-08-08.md`. When older market
mockups or historical UI notes conflict with that baseline, use the 2026-08-08
baseline.

## Goal

Turn the standardized market from a custody-operation panel into a compact
trading terminal. The player researches a catalog item, compares real market
data, fills an explicit order ticket, and confirms. Internal custody remains a
settlement guarantee, not a routine player task.

## Player-facing asset model

The current personal Base Vault is the active account inventory. A future
authorized AE warehouse resolver has higher priority, then falls back to Base
Vault. The terminal never reads the held item or player inventory.

For a sell order, the terminal displays account-warehouse sellable quantity.
When the player confirms an order, the service atomically reserves the required
items into market escrow. Cancelling an unfilled quantity returns it to the
resolved account warehouse. This removes the manual `deposit -> AVAILABLE`
step without allowing an open order to point at movable inventory.

For a buy order, bank funds are frozen as today. A completed purchase attempts
delivery to the resolved account warehouse. Only a full or unavailable target
creates a visible pending-delivery recovery action; normal buys do not require
a manual claim button.

`AVAILABLE`, `ESCROW_SELL`, `CLAIMABLE`, and frozen funds remain internal audit
states. They are exposed only through concise position summaries and recovery
details, never as the main player workflow.

## Three views

### 1. Browse

- Fixed search, filter, and paging controls above a four-column item grid.
- Every item card uses the real ItemStack icon and shows name, last/reference
  price, period change arrow/percentage, compact sparkline, and liquidity
  status.
- Only the grid scrolls. Search and page controls do not move.
- Empty or inactive markets show an explicit low-liquidity state rather than a
  fabricated chart.

### 2. Hover quote

- Appears only after the pointer remains on a real item card briefly.
- Shows latest price, 24-hour change and volume, best bid/ask, account sellable
  quantity, funds available, and a larger real price sparkline.
- It is read-only, clipped to terminal bounds, and disappears on scroll,
  click, popup, refresh, focus loss, or view transition.

### 3. Detail and order ticket

- Full-page trading workstation, entered by clicking a browse item.
- Header: item icon/name, standard unit, last/reference price, 24-hour change,
  volume, and a `1h / 24h / 7d` range selector.
- Main chart: real OHLC candles plus volume bars when the server has enough
  executions; otherwise a labelled real trade line or empty state.
- Order book: bid, last trade, and ask columns. Selecting a bid/ask populates
  the editable order ticket only; it never submits an order.
- Order ticket: `Buy / Sell`, `Market / Limit`, editable quantity and limit
  price, estimated cost/proceeds, fee, available bank funds or sellable account
  inventory, and a single confirm button.
- Footer: concise open-order and pending-delivery summaries. Detailed recovery
  actions appear only when there is an actual pending asset or cancellable
  order.

## Required data and execution changes

1. Add batched real trade aggregation for `1h`, `24h`, and `7d`: OHLC buckets,
   volume, and trade count. No candle or trend is generated when the source
   data is absent.
2. Keep real order-book depth and latest trade data; clicking a level fills
   editable quantity/price defaults.
3. Add an account inventory resolver interface. v1 resolves to Base Vault;
   later it can prefer an authorized AE warehouse without changing the market
   order contract.
4. Replace manual standardized deposit with a sell-side reserve operation at
   order submit. Existing custody is retained as an implementation detail and
   recovery domain.
5. Replace routine claim with automatic delivery after buy execution. A failed
   delivery remains durable and actionable.

## Constraints

- The UI must not promise a quantity that cannot be internally reserved.
- No direct read of player inventory or current hand from a market path.
- Market, limit, cancellation, delivery, and recovery actions use a shared
  request id and remain auditable.
- Standardized market is the reference implementation; custom and exchange
  reuse visual structure but retain their own settlement rules.

## Delivery order

1. Introduce the account inventory resolver and automatic sell reserve / buy
   delivery contract with focused recovery tests.
2. Add real OHLC and volume aggregates plus API/snapshot serialization.
3. Rebuild standardized detail as chart, order book, and editable trade ticket.
4. Rework browse cards and hover to surface real movement and liquidity.
5. Migrate custom and exchange to the common shell without forcing stock-market
   terminology onto their different rules.

## 2026-08-08 implementation status

The standardized market now uses the account-inventory boundary in its primary
order path. Sell orders reserve the selected catalog product from Base Vault,
move it into the existing custody ledger, and create the escrowed order inside
the shared Vault transaction. Immediate-sell remainders and cancelled sell
orders attempt to return to Base Vault. Buy executions attempt automatic Vault
delivery; only failed delivery remains pending in the existing claim/recovery
domain. Legacy deposit and claim routes remain wire-compatible but are no
longer presented as the normal detail-page workflow.

The browser now has a fixed 4 x 3 grid, compact query/filter/sort controls,
real ItemStack icons, real page-level trade summaries, percentage movement,
sparklines, liquidity status, and Base Vault sellable quantity. Hover uses the
same real projection and collapses its chart area when fewer than two trade
points exist.

The detail page now uses the horizontal chart/book/ticket budget. The order
ticket has explicit side, market/limit type, quantity, optional price,
estimated value, account source, and one unified confirmation action. The
server still recalculates quantity, price, balance, fee, and inventory before
execution. The footer is expressed as account inventory, current orders, and
exceptional pending delivery rather than routine custody operations.

The read projection and PostgreSQL indexes required for page-level prices,
best bid/ask, volume, turnover, trade count, and chart buckets are present.
The detail chart now has an interactive `1h / 24h / 7d` selector. Each refresh
uses the repository's real time buckets and serializes bucket close, volume,
and start time through the compatible price-point contract. Full OHLC bodies
and wicks, clickable book-price autofill, and a true maximum-quantity shortcut
remain follow-up work; the UI must not label those capabilities as complete
until those contracts are added.
