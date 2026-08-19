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

## 2026-08-18 live-test confirmation: continuous-book execution semantics

The 2026-08-17/18 bulk-order test confirmed that the standardized market is a
continuous limit-order book. The following behavior is part of the formal
trading design rather than an incidental implementation detail.

### Limit price is a boundary, not a forced execution price

- A buy limit is the maximum acceptable unit price. It may execute at that
  price or any lower price.
- A sell limit is the minimum acceptable unit price. It may execute at that
  price or any higher price.
- `last trade`, `best bid`, `best ask`, and the player's entered limit are four
  different values. The last trade is historical market data and never
  promises that the same price remains executable.

### Aggressive limit orders sweep the resting book level by level

An incoming marketable order consumes the best eligible resting orders in
price priority and, within one price, time priority. Every fill uses the price
of the resting counter-order, not the incoming order's worst acceptable limit.

Example: the resting asks are `90 x10`, `91 x20`, and `95 x30`. An incoming
buy limit `100 x100` executes as:

```text
10 @ 90
20 @ 91
30 @ 95
```

The first 60 units therefore do not all execute at 100. The unfilled 40 units
then rest as a buy order at 100.

### Risk of an aggressive remainder resting at the limit

After the initial sweep, the remainder is genuine displayed buying interest at
the entered limit. If later external sell orders are marketable against it,
the book presents the highest eligible bid first. The resting `100 x40` in the
example can therefore be filled at 100, up to its remaining 40 units, unless it
is filled or cancelled first.

This is a bounded obligation, not unlimited exposure:

- execution cannot exceed the order's remaining quantity;
- buy funds are frozen for the order contract;
- a later sell wave larger than the remainder continues to lower bids after
  this order is filled;
- a better external bid has price priority, and equal-priced orders retain time
  priority;
- cancellation only stops future execution of the still-open remainder and
  cannot reverse completed fills.

### Resting-price rule and self-match prevention

The execution price is the selected resting order's unit price. Consequently:

| Resting order | Incoming order | Execution price |
| --- | --- | ---: |
| buy 90 | sell limit 80 | 90 |
| sell 80 | buy limit 90 | 80 |
| sell 90 | buy limit 100 | 90 |
| buy 100 | sell limit 90 | 100 |

Orders owned by the same authenticated player are not eligible counterparties.
The matcher skips them without changing their price, quantity, custody, frozen
funds, or priority, and continues to the next eligible external order. If only
self-owned crossing interest exists, both limit orders remain open and no
trade, fee, settlement, or delivery record is created.

### Relationship to U.S. equity-market behavior

The core semantics intentionally resemble a conventional U.S. continuous
limit-order book:

- the SEC describes a buy limit as executable at the limit or lower and a sell
  limit as executable at the limit or higher;
- Nasdaq documents price/time priority for displayed limit orders and provides
  self-match-prevention functionality;
- Nasdaq examples also show an incoming more-aggressive buy executing at the
  lower price of the order already resting on its book.

This project is not a simulation of the complete U.S. National Market System.
It does not claim NBBO protection or cross-venue routing, opening/closing and
halt auctions, short-sale regulation, price collars, hidden/reserve order
priority, or the full set of exchange order attributes. The comparison is
limited to continuous-book limit semantics, resting-price execution,
price/time priority, and self-match prevention.

Official reference points:

- SEC, `Limit Orders`: <https://www.sec.gov/answers/limit.htm>
- Nasdaq, `The Nasdaq Stock Market`: <https://nasdaqtrader.com/trader.aspx?id=tradingusequities>
- Nasdaq Equity 4 rulebook examples: <https://listingcenter.nasdaq.com/rulebook/nasdaq/rules/Nasdaq%20Equity%204>

## 2026-08-18 design direction: 24x7 auctions and circuit breakers

The standardized market is intended to remain available continuously. Players
may trade at any time, and future AE or other machine-facing adapters may
submit orders without a human continuously watching the terminal. This makes
exchange-style safeguards more important, but it does not create a reason to
copy a daily stock-market open and close.

### Where call auctions belong in a 24x7 market

Routine trading remains the continuous price/time-priority book. A call auction
is reserved for transitions where the market needs to discover one defensible
price before continuous matching resumes:

- the first activation of a newly admitted standardized product;
- reopening after a product volatility pause;
- reopening after a matcher, database, bank, Vault, or delivery outage;
- a material catalog, unit, or trading-rule change;
- recovery after a long period without a trustworthy traded reference.

During an auction collection period, orders may be entered and cancelled but
no trade executes. The terminal must publish the indicative clearing price,
paired quantity, unmatched quantity and side, reference price, price collar,
and remaining collection time.

The clearing algorithm must be deterministic and server authoritative:

1. select the price that maximizes executable quantity;
2. if tied, select the price that minimizes unmatched imbalance;
3. if still tied, select the price nearest the last trustworthy reference;
4. apply a final deterministic side/price rule if a tie still remains;
5. allocate eligible interest by price priority and then time priority;
6. execute every auction fill at the one clearing price;
7. return unfilled eligible limit quantities to the continuous book without
   changing their economic limit; non-persistent auction-only orders expire.

Orders remain fully funded or asset-reserved throughout collection. Self-owned
crossing interest is ineligible in the clearing calculation. Cancellation is
always allowed before the auction freeze boundary and must release only the
unfilled reserve.

### Product-level volatility protection

A circuit breaker is a temporary matching state, not a trade reversal. Fills
already committed remain final. A product moves through explicit states:

```text
CONTINUOUS -> LIMITED -> PAUSED -> AUCTION -> CONTINUOUS
```

- `CONTINUOUS`: normal price/time matching.
- `LIMITED`: the executable price has reached a dynamic band; no execution may
  occur outside the band while liquidity has a short opportunity to normalize.
- `PAUSED`: no trades execute. Cancels remain available and new limit orders may
  join the reopening auction; immediate/market-style orders are rejected.
- `AUCTION`: indicative price and imbalance are published, then one clearing
  price is calculated. If the result remains outside its collar or materially
  unstable, collection is extended instead of forcing a print.

The first release must not use the last tiny trade as the sole reference. The
trusted reference hierarchy should prefer a sufficiently liquid rolling
volume-weighted or robust median price, then the last trusted auction price,
then an older trusted reference. A catalog baseline may bootstrap a new or
illiquid product only with wider safeguards and explicit `REFERENCE` status.

Minimum trade count, quantity, and notional gates are required before a recent
window can move the reference. This prevents a one-unit print from moving the
band or triggering a pause. Band widths must be tiered by actual liquidity and
volatility. Candidate percentages are configuration inputs, not design facts;
the system should first run in shadow mode and record hypothetical limits and
pauses before enforcement values are approved.

A practical initial pause can be two to five minutes, followed by a reopening
auction. Continued movement of the indicative price or excessive imbalance may
extend collection in short deterministic increments. These durations also
remain configurable and require live-data calibration.

### Market-wide and operational halts

The U.S. market-wide `7% / 13% / 20%` thresholds are tied to a mature broad
index and a trading day. They are not suitable defaults for this market, which
has neither a mature composite index nor a daily close.

The first global breaker should therefore protect operational integrity rather
than react to a synthetic economy index. The standardized market may enter a
global `PAUSED` or `DEGRADED` state when it cannot prove safe settlement, for
example:

- the authoritative database or transaction runner is unavailable;
- bank freeze, settlement, or release invariants fail;
- Base Vault or a future AE adapter cannot prove reservation ownership;
- matcher recovery finds an unresolved partial operation;
- market data is stale enough that an immediate-order preview is unsafe;
- an administrator activates an audited emergency kill switch.

The narrowest safe scope must be used: pause one product or one asset adapter
before pausing the whole market. Completed trades are not silently rolled back.
Recovery actions remain audited, and reopening proceeds through a call auction
when the prior book or reference may be stale.

A broad economic circuit breaker may be considered only after the market has
enough liquid products for a manipulation-resistant index and enough history
to calibrate it. It must not be based on a simple average of sparse last trades.

### Pre-trade controls are the first line of defense

Most erroneous orders should be stopped before matching rather than delegated
to a circuit breaker. Server-side checks should include:

- maximum order quantity and notional value;
- maximum open quantity, frozen funds, and reserved inventory per player and
  product;
- price-deviation warnings and a hard protection boundary for fat-finger input;
- immediate orders represented as IOC-style marketable limits with an explicit
  maximum slippage, never an unbounded price instruction;
- bounded order, replace, and cancel rates, with clear reject reasons;
- self-match prevention and immutable authenticated ownership;
- account, product, adapter, and market kill switches.

The terminal must distinguish `CONTINUOUS`, `LIMITED`, `PAUSED`, `AUCTION`, and
`DEGRADED` in text as well as color. A rejected or paused order must name the
rule, current reference/band, affected product, and safe next action.

### AE and machine-adapter boundary

An AE or other machine-facing adapter does not call the matcher directly. It
passes through the same authenticated order contract and adds stricter
automation controls:

- adapter-scoped identity, permissions, idempotency key, sequence/version, and
  complete audit trail;
- atomic proof of funds or physical-item reservation before book admission;
- per-adapter product, quantity, notional, open-order, and message-rate limits;
- heartbeat leases and optional cancel-on-disconnect for non-persistent
  automated orders;
- explicit opt-in for persistent automated orders rather than persistence by
  accident;
- an account owner kill switch that stops new automation and cancels the
  adapter's still-open orders without touching completed trades;
- surveillance for extreme cancel ratios, quote stuffing, layering/spoofing,
  related-account wash trading, and tiny trades intended to mark the reference.

Human GTC-style player orders may remain open through logout. Automated session
orders should default to leases or cancel-on-disconnect unless explicitly
created as persistent orders. If machine speed later materially disadvantages
human players, a small frequent-batch auction is a possible future fairness
tool, but it is not part of the first circuit-breaker implementation.

### Phased delivery

1. Add pre-trade price, size, notional, exposure, and rate controls plus an
   audited manual kill switch.
2. Add IOC-style maximum-slippage protection for immediate orders.
3. Add product market states and shadow-mode dynamic-band observation.
4. After calibration, enforce `LIMITED / PAUSED` and implement the reopening
   auction with indicative price and imbalance.
5. Apply heartbeat leases, cancel-on-disconnect, limits, and kill switches
   before enabling AE or other machine order entry.
6. Add surveillance and only later evaluate a robust cross-product index or
   frequent-batch matching.

Official design references:

- LULD Plan: <https://www.luldplan.com/>
- NYSE Market-Wide Circuit Breakers FAQ: <https://www.nyse.com/publicdocs/nyse/NYSE_MWCB_FAQ.pdf>
- Nasdaq Halt Cross: <https://classic.nasdaqtrader.com/content/productsservices/trading/ipohalt/HaltCross_factsheet.pdf>
- Nasdaq Pre-Trade Risk Management: <https://www.nasdaqtrader.com/TraderNews.aspx?id=hta2008-002>

## 2026-08-18 exchange-grade controls and settlement policy

The matching engine is only the core of an exchange. Before standardized-market
automation is treated as production-grade, the surrounding operating system must
also cover rules, risk, surveillance, clearing evidence, deterministic data, and
resilience. The existing service already provides a useful foundation: authenticated
server-side ownership, prefunded buy orders, asset-reserved sell orders, price/time
priority, resting-order execution prices, self-match prevention, idempotent request
IDs, custody/recovery states, and an auditable order/trade history.

The following capabilities are still exchange hardening requirements rather than
claims about the current implementation:

1. **Versioned rulebook and product contracts**
   - Define tick size, lot size, units, quantity and notional bounds, supported
     time-in-force, amendment priority, fees, bands, auction rules, and product
     suspension or retirement.
   - Persist the applicable catalog and trading-rule version on every accepted
     order and trade. Sensitive administrative changes require effective times,
     audit records, and separated approval authority.
2. **Pre-trade risk gateway**
   - Enforce order, account, product, and adapter limits for quantity, notional,
     price deviation, aggregate open exposure, active-order count, and message
     rate before an instruction reaches the matcher.
   - Provide account, product, adapter, and market kill switches. AE and other
     machine sessions additionally require leases or cancel-on-disconnect.
3. **Authoritative event sequence and market-data recovery**
   - Assign a monotonic server sequence to accepted orders, rejects, cancels,
     fills, settlement events, delivery transitions, and market-state changes.
   - Publish snapshot plus incremental data. A detected sequence gap must force a
     snapshot refresh instead of allowing a client or adapter to continue from a
     partial book.
4. **Independent reconciliation**
   - Continuously prove that order quantities, frozen funds, fees, seller credits,
     escrow inventory, buyer entitlements, Vault deliveries, and recovery records
     conserve value and quantity.
   - The reconciler must be independent of the command path that creates those
     records and must raise an auditable exception rather than silently repair it.
5. **Surveillance and enforcement workflow**
   - Detect related-account wash trading, prearranged matching, spoofing/layering,
     quote stuffing, abnormal cancel ratios, and tiny prints intended to move a
     reference price. Same-account self-match prevention is necessary but cannot
     detect alternate accounts or coordinated players.
   - Findings first create evidence-backed cases for review. Automatic sanctions
     require explicit policy, thresholds, appeal handling, and auditability.
6. **Operational resilience**
   - Define capacity tests, deterministic crash replay, failover behavior, RPO/RTO,
     scheduled maintenance rules, incident communication, and repeated recovery
     exercises for matcher, PostgreSQL, bank, Vault, and future adapters.
7. **Adapter admission and conformance**
   - Machine access uses scoped identities, protocol versions, certification tests,
     heartbeats, quotas, execution reports, and an independent drop-copy stream.
     It never calls matcher internals directly.
8. **Erroneous-trade and dispute rules**
   - Define objective review thresholds, authority, time limits, evidence, and
     correction procedures. Original trades are never deleted or silently edited;
     corrections use linked reversal, compensation, or recovery events.

Recommended order of delivery is: risk gateway, global event sequence, kill
switches and independent reconciliation before AE order entry; then surveillance,
formal rule governance, dispute handling and resilience drills; only after those
foundations should enforced bands, reopening auctions, or liquidity incentives be
expanded.

### Current settlement model versus T+1

`T` is the trade date. `T+1` means that the contractual exchange of cash and
securities is completed no later than the next business day; it does not mean that
matching waits until the next day. The United States changed the standard settlement
cycle for most broker-dealer securities transactions from T+2 to T+1 on 2024-05-28.
A retail brokerage may display the fill and update buying power immediately while
the broker, clearing agency, and depository complete final settlement on T+1.

The standardized GTNH market intentionally does not copy that delay. Its present
model is **prefunded immediate ledger settlement with recoverable physical delivery**:

1. A buy order freezes its maximum required bank funds before book admission; a
   sell order reserves the real standardized item in market escrow before admission.
2. When orders match, the same server-side transaction settles the gross amount from
   frozen buyer funds to the seller, posts fees, updates both orders and sell escrow,
   records the trade, and creates the buyer's `CLAIMABLE` custody entitlement.
3. The terminal then attempts delivery of that entitlement to the buyer's Base Vault.
   If capacity or delivery is unavailable, the completed trade and cash settlement
   remain final while the item stays `CLAIMABLE` for explicit claim/recovery.

Buyer fees follow a **reserve now, charge only on fill** contract. Book admission
reserves limit-price principal plus the maximum taker fee for the full quantity, but
that fee capacity is not tax income and no fee transaction exists before execution.
Each fill charges only its actual maker/taker fee. Price improvement, unfilled
quantity, and unused fee capacity remain frozen for the still-open order and are
released when the order completes or its remainder is cancelled. Seller fees continue
to be deducted from actual sale proceeds and therefore need no separate pre-funding.

The terminal must call this amount `资金预留` rather than implying that it has already
been spent. Confirmation and receipts distinguish order principal, maximum fee
capacity, actual charged fee, and the remainder that will be released. Allowing an
unfunded future buyer fee is prohibited: the player could spend the balance before a
resting order fills and make an unrelated seller's valid order fail.

Any historical resting buy whose remaining reservation cannot cover remaining
limit-price principal plus the maximum buyer fee is not eligible liquidity. The
matcher quarantines it as `EXCEPTION`, releases its residual reserve with an
idempotent bank request, records the reason, and continues to the next eligible order.
Startup/operational audit checks both `OPEN` and `PARTIALLY_FILLED` orders and also
proves that each owner's aggregate bank frozen balance covers active order reserves.

Economically this is closer to a T+0, delivery-versus-payment design at the bank and
custody-ledger boundary than to U.S. T+1 settlement. It is not correct to describe
the later Base Vault placement as an unsettled trade: the buyer already owns an
audited custody entitlement. Introducing an artificial T+1 delay would add exposure
and player confusion without solving a present constraint, so it is out of scope.

Official settlement reference:

- SEC, `Shortening the Securities Transaction Settlement Cycle`:
  <https://www.sec.gov/investment/settlement-cycle-small-entity-compliance-guide-15c6-1-15c6-2-204-2>

### Deferred complex financial products

The following products are explicitly **not current scope**. They remain a future
research register so later demand can be evaluated without implying a commitment:

- **Short selling** requires a formal borrow/locate source, ownership and recall
  rules, collateral, borrow fees, buy-ins for failed delivery, and manipulation
  controls. It must not be simulated by allowing negative Vault inventory.
- **Margin or leverage** requires credit underwriting, continuous mark-to-market,
  maintenance margin, liquidation priority, insolvency/default handling, and a
  documented loss waterfall. It must not be simulated by allowing negative bank
  balances or unfunded orders.
- **Options** require standardized contracts, expiry calendars, strike and exercise
  rules, collateral for writers, assignment, settlement, price/risk models, and
  treatment of product/catalog changes. They must remain separate from the spot
  order model.

These instruments may be reconsidered only after the fully funded spot market has
stable risk controls, reconciliation, surveillance, event sequencing, and sufficient
liquidity. Until then, the standard market remains long-only, unleveraged, fully
funded, physically reserved spot trading.

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
