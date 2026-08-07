# Current Product Direction And Gap Review

Date: 2026-05-18

This note records the current product direction confirmed after the terminal usability pass and the first real ServerTools cross-server warp validation.

## Current Direction

`JsirGalaxyBase` terminal should now be treated as a comprehensive intelligent console for the GTNH group server, not as a single page or a visual skin.

The next product direction has three main tracks:

1. ServerTools and group-server productization
   - Keep the backend command chain stable.
   - Move the usable player entry into the terminal as a group-server page.
   - Surface server state, warp choices, transfer status, errors, and history in one place.
2. Market productization
   - Continue the three-market split: standardized market, custom market, exchange market.
   - Turn the terminal market pages into real player workflows, not only descriptions and early test panels.
   - Validate the business chain behind each page.
3. Terminal shell intelligence
   - The terminal shell is the shared UX layer for status, routing, notifications, confirmations, error explanations, and future permission/career/reputation gates.
   - Cosmetic tuning should continue only when it blocks usability or business workflows.

## Progress Review

### Terminal Foundation

Done:

- G key and inventory button now use the server-authorized client screen open chain.
- BetterQuesting-style terminal framework is the formal player entry.
- Home shell, section host, bank page, market root, standardized market, custom market, and exchange market are already migrated into the new shell.
- Old terminal ModularUI transition layer has been removed from the production terminal path.
- Recent usability work made the layout denser, reduced title/header noise, added auto-reveal navigation behavior, and hid HUD/chat bleed behind the screen.

Remaining:

- Add a formal group-server / ServerTools page.
- Keep polishing actual page content density through real mouse browsing, especially long lists, button visibility, and scroll areas.
- Add more player-facing state explanations: why an action is disabled, what server the player is on, what the last transfer did, and how to recover from a failed action.

### ServerTools And Cluster

Done:

- Phase 1 command/service/repository chain exists for `home`, `back`, `spawn`, `tpa`, `rtp`, and `warp`.
- PostgreSQL is the source of truth for homes, back records, warps, TPA, RTP, server directory, and transfer tickets.
- Phase 2 cluster ticket lifecycle is no longer a placeholder: source server dispatches, target server restores the landing point, and tickets move to terminal states.
- Phase 3 gray chain is running on Docker + supervisor for Entrance, Lobby, and S2 while S1 stays outside the gray proxy path.
- `/jgbst warp list`, `/jgbst warp s2test`, and `/jgbst warp lobbytest` were tested in game and completed real Lobby <-> S2 transfer tickets.
- The timeout issue found in real testing was fixed by increasing the transfer ticket TTL and Velocity read timeout.

Remaining:

- Build the terminal group-server page on top of the current backend.
- First page scope should expose at least: current server, known servers, warp list, selected warp details, transfer action, recent ticket status, and refresh.
- Decide whether the first GUI scope includes only warp or also home/back/spawn/rtp/tpa.
- Add clear player-facing error states for missing warp, disabled server, expired ticket, gateway dispatch failure, and target-server restore failure.
- Add admin/maintenance flow for system warps and server directory after the player flow is stable.
- Run broader in-game validation for home/back/spawn/tpa/rtp across the gray chain.

### Market

Done:

- The formal architecture is already split into three product lines:
  - standardized market
  - custom market
  - exchange market
- MARKET root is documented as a three-market entrance, not a mixed transaction page.
- Standardized market has early order/custody/claim/recovery skeletons.
- Exchange market has a quote/exchange v0 path backed by bank settlement.
- Custom market has minimal listing-chain design and related implementation history in docs/migrations.
- Terminal pages for MARKET root, standardized, custom, and exchange are already present in the new shell.

Remaining:

- Re-verify the actual business completeness of each market through the current code and in-game flow.
- Standardized market still needs a confirmed formal catalog/admission boundary instead of relying on temporary GT material assumptions.
- Custom market needs an end-to-end player workflow review: list item, browse, inspect, buy, cancel, claim/deliver, audit trail.
- Exchange market needs the formal rules layer: pairs, versioned rules, limits, fees/taxes, reserves, and audit.
- Market terminal pages should become workflow-first pages with visible actions, confirmation dialogs, snapshot refresh, bank balance invalidation, and clear result/error feedback.
- Admin/audit/recovery views are still product requirements, not finished player-facing flows.

## Recommended Next Phases

### Phase A: ServerTools Terminal Page

Goal: make group-server travel usable from the terminal without requiring chat commands.

Recommended first scope:

- Add a `GROUP_SERVER` or `SERVER_TOOLS` terminal page.
- Show current server and known enabled servers.
- Show system warps from `server_warp`.
- Let the player select a warp and trigger the same backend chain as `/jgbst warp <name>`.
- Show recent `cluster_transfer_ticket` status for the player.
- Keep bottom/global buttons stable: refresh, close, and page-level primary action.

Open product decisions:

- Page name in Chinese: `群组服`, `服务器`, `传送`, or a more thematic name such as `星门`.
- First action set: warp only, or warp plus home/back/spawn/rtp/tpa.

### Phase B: Market Root Usability And Validation

Goal: make the market pages usable as real player workflows.

Recommended first scope:

- Ensure MARKET root always exposes the three child entrances above the fold.
- Validate standardized/custom/exchange pages with real terminal clicks.
- For each action, ensure confirmation, snapshot refresh, bank balance invalidation, and result feedback are coherent.

### Phase C: Market Business Hardening

Goal: finish the business meaning behind the UI.

Recommended scope:

- Exchange rules layer.
- Standardized catalog and admission boundary.
- Custom listing full chain and audit.
- Recovery/admin views for stuck assets and abnormal trades.

### Phase D: Intelligent Terminal Shell

Goal: make the terminal feel like a smart system console.

Recommended scope:

- Unified event/status center.
- Permission and rule explanation surface.
- Transfer and market failure diagnosis.
- Future career, contribution, reputation, and public-service gates.

## Document Status Notes

- `docs/terminal-phase7-handover-to-chatgpt5.5.md` is now a historical handover/prompt document. Its Phase 7 status is older than the current README and terminal plan.
- `docs/servertools-phase3-gray-rollout-status-2026-05-17.md` originally said no player-level cross-server teleport had been exercised. That note was correct for 2026-05-17, but it is now superseded by the 2026-05-18 Lobby <-> S2 warp validation.
- `docs/market-three-part-architecture.md` remains valid as the product boundary. Some status statements inside older market docs may be historical and should be checked against current code before execution.

## Confirmation Checklist

Please confirm these decisions before the next implementation prompt:

1. The next major UI page is the group-server / ServerTools terminal page.
2. The first terminal ServerTools scope starts with warp, then expands to home/back/spawn/rtp/tpa.
3. Market work should prioritize real workflow validation and page usability before adding large new market features.
4. S1 remains outside the gray chain until explicitly approved.
