# Market Browser Hover Detail Redesign V1

## Goal

All three market types share one interaction contract:

1. Browse: a fixed four-column item grid, with search, filters and pagination outside the scrolling grid.
2. Compare: a passive hover card shows real market summary and, when available, real execution history.
3. Act: clicking an item opens an independent detail mode for orders, custody, claimable assets and actions.

The browse surface is for discovery and density. The detail surface is for irreversible actions. Help belongs in the terminal help popup, not the working surface.

## Layering and bounds

- The only browse scroll target is the item grid.
- Hover is read-only, does not receive clicks and is clamped inside the terminal content bounds.
- A click, scroll, mouse leave or modal popup closes hover immediately.
- Rendering order is body, hover overlay, then modal popup.
- `DETAIL` is client state only. It reuses the existing selected-product snapshot and action chain; terminal back returns to `BROWSE` with query, page and grid position retained.

## Standard market first

The standard market is the reference implementation. Its catalog page supplies a structured row for every directory item: icon reference, display name, reference price, latest trade, best bid/ask, 24h volume, personal custody values and up to twelve real execution price points. The service reads execution data in page batches; it does not fabricate a sparkline where there is no history.

## Migration order

1. Standard market: browse grid, hover card and detail route.
2. Custom market: adapt listings into the same browse row model and detail contract.
3. Exchange market: adapt exchange pairs into the same browse row model and quote detail contract.

The market-specific services retain their own business rules. This redesign does not merge standard custody, custom listing escrow or exchange settlement semantics.

## Phase 3 implementation boundary

- `MARKET_CUSTOM` now uses the same client-side browse/detail controller as the standard market: the browse route is a four-column listing grid and the detail route is only opened after the selected listing snapshot returns. Its three browse scopes remain `active`, `selling` and `pending`; selecting a scope refreshes the authoritative server snapshot rather than filtering stale client rows.
- `MARKET_EXCHANGE` uses the full `TaskCoinCatalog` as its browse source. The catalog is discoverability only: clicking a coin keeps the existing formal target and quote-confirmation gate, and execution still validates the actual item held by the player on the server.
- Custom listings show their listing price, counterparty and delivery state. Exchange pairs show the real quote, rule version, limit status and held-input match. Neither page borrows the standardized market's order book, custody labels or price chart when the underlying service does not provide those facts.
- Shared `MarketBrowseDetailController` keeps browse query, page, scroll position and selected key local to the client. Returning from detail restores browse context; route changes and snapshot rebuilds do not leave a detail-only hover overlay behind.

## Phase 3 server-authoritative browse completion

- `MARKET_CUSTOM` and `MARKET_EXCHANGE` now receive a structured `TerminalMarketBrowseEntry` page from the server. The client renders rows only; it no longer parses listing strings as the primary source or locally rebuilds the task-coin catalog.
- Custom page context (`scope`, query, page and selected listing) and exchange page context (query, page and selected coin) remain in their existing action payloads. The selected exchange coin is presentation context only: final quote and execution continue to validate the actual selected Base Vault slot server-side.
- The shared packet format carries row identity, item identity, title, subtitle, primary value, status and page boundaries for both non-standard markets. This retains their distinct business semantics while giving all three markets the same browse/detail transport contract.

## Phase 3 semantic hardening

- The shared browse model now carries a market kind. Standardized cards retain real intraday price, order-book and custody fields. Custom listing cards instead show listing price, counterparty or delivery context and listing state. Exchange cards show a task-coin face value, family/tier and Vault-execution eligibility. Neither non-standard market renders invented bid/ask, custody labels or historical charts.
- Hover follows the same boundary: a custom listing hover is a passive listing summary, and an exchange hover is a passive coin/quote eligibility summary. Both close before route changes and never survive into detail mode.
- Custom browse exposes the existing scopes and a price field alongside the publish command so publishing is not a visual dead end. Selecting a scope performs a server refresh without accidentally requesting a detail route for an empty listing id.
- The custom browse adapter is still backed by the legacy service snapshot arrays for compatibility. Replacing those arrays at their source with a dedicated listing query projection is retained as the next server-side cleanup; the client no longer treats them as its primary UI contract.

## Acceptance criteria

- Four fixed columns at terminal target width; no permanent detail column during browse.
- Search/pagination do not move while the grid scrolls.
- Hover never crosses the terminal bounds or appears above a modal confirmation.
- Empty or one-point history explicitly states that historical pricing is unavailable.
- Detail mode uses the existing confirmed server selection before presenting trading actions.

## Refresh and response ordering closeout

- Every new terminal action carries a client request sequence. The server echoes it on the resulting snapshot; old packets without the appended field remain decodable as sequence `0` during the compatibility window.
- The client applies snapshots monotonically. A delayed browse, quote or automatic-refresh response cannot replace a newer page selection or detail response.
- Automatic refresh is presentation refresh only. Exchange refresh does not silently create a new formal quote; quote generation remains an explicit player action with its existing server gate.
- While a market input owns focus, a modal confirmation is open, or the client has left the page, automatic refresh must not rebuild the working surface. This protects typed quantity, price, scope and selected-asset context.
- Player-facing text must describe products, account warehouse inventory, pending delivery and quote availability. Protocol class names and internal custody states remain audit vocabulary only.
