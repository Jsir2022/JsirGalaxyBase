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

## Acceptance criteria

- Four fixed columns at terminal target width; no permanent detail column during browse.
- Search/pagination do not move while the grid scrolls.
- Hover never crosses the terminal bounds or appears above a modal confirmation.
- Empty or one-point history explicitly states that historical pricing is unavailable.
- Detail mode uses the existing confirmed server selection before presenting trading actions.
