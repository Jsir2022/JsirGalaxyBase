# Terminal ServerTools Page V1 Acceptance Review

Date: 2026-06-06

This review covers the current `SERVER_TOOLS` terminal page implementation that appeared in the working tree after the product-direction review.

## Acceptance Result

The ServerTools terminal page is no longer a missing feature. A first version has been wired into the new terminal shell.

Accepted as present:

- `TerminalPage.SERVER_TOOLS` exists as a top-level terminal page.
- Navigation includes the ServerTools page with the `传送 / 群组服` label.
- Terminal actions exist for refresh, warp selection, and warp confirmation:
  - `SERVER_TOOLS_REFRESH`
  - `SERVER_TOOLS_SELECT_WARP`
  - `SERVER_TOOLS_CONFIRM_WARP`
- Snapshot, payload, client view model, network serialization, and client section classes exist:
  - `TerminalServerToolsActionPayload`
  - `TerminalServerToolsSectionSnapshot`
  - `TerminalServerToolsSectionModel`
  - `TerminalServerToolsSection`
  - `TerminalServerToolsSectionState`
- The default server-side facade reads live data through the current runtime:
  - `ServerToolsModule`
  - `PlayerTeleportService.listWarps()`
  - `ClusterInfrastructure.getServerDirectory().listAll()`
- Confirming a warp reuses the existing real backend path:
  - `PlayerTeleportService.prepareWarpTeleport(...)`
  - `ServerToolsModule.dispatchTeleport(...)`
  - no duplicate teleport business logic was introduced in the terminal layer.
- The client opens a confirmation popup before sending `SERVER_TOOLS_CONFIRM_WARP`.

## Remaining Gaps

These items are still open after the 2026-06-06 close round.

## 2026-06-07 Direction Update

After later in-game comparison against the intended transport concept mockup, the remaining work should no longer be treated as “density-only tweaks”.

Updated conclusion:

- v1 successfully proves that the page exists and is wired to the real warp backend
- v1 should **not** be considered structurally close to final product quality
- the next UI round should move from generic section-style presentation to a dedicated transport workflow layout

What changed in the judgment:

1. The main gap is now page structure, not backend availability.
2. The target page is a three-region workflow tool:
   - left navigation
   - middle warp list
   - right detail/status/action panel
3. The current v1 page still behaves too much like a mixed terminal content section.
4. The overlap discovered around the warp list and footer is a symptom of the broader layout-model mismatch.

So the next coding round should not just “trim some padding” inside the current v1 section. It should keep the real ServerTools action chain, but rebuild the transport page presentation to match the concept direction recorded in:

- `docs/terminal-ui-redesign-toward-concept-mockup-2026-06-07.md`

### 1. In-game terminal click validation still pending

The page needs a real mouse-browse validation pass.

Manual flow:

1. Open terminal with `G`.
2. Hover the left edge and open navigation.
3. Enter `传送 / 群组服`.
4. Select `s2test`.
5. Confirm the popup.
6. Verify transfer to S2.
7. Reopen terminal on S2.
8. Select `lobbytest`.
9. Confirm the popup.
10. Verify transfer back to Lobby.

Expected DB check:

```sql
SELECT ticket_id, request_id, player_name, source_server_id, target_server_id,
       status, status_message, created_at, updated_at
FROM cluster_transfer_ticket
ORDER BY created_at DESC
LIMIT 20;
```

### 2. UI structure should be checked against the target concept

The first implementation uses three internal areas:

- server/warp browser
- selected warp detail
- action/feedback

This is a reasonable v1 structure for backend exposure, but it is not yet the target product structure.

During in-game review, check:

- whether the page is truly split into stable workflow regions
- whether the warp list behaves like a selectable list instead of a text block
- whether the selected detail panel is visually separate from the list
- whether the confirm action dominates the page clearly
- whether footer controls are still competing with transport interaction

## Next Development Prompt

Use this prompt for the next coding AI if code work continues:

```text
You are continuing JsirGalaxyBase terminal ServerTools page work.

Context:
- A first `SERVER_TOOLS` terminal page already exists.
- Do not rewrite the page from scratch.
- Keep the existing terminal shell, action/snapshot protocol, and ServerTools backend chain.
- Confirm warp must continue to reuse `PlayerTeleportService.prepareWarpTeleport(...)` and `ServerToolsModule.dispatchTeleport(...)`.

Primary goal:
Rebuild the existing ServerTools terminal page v1 presentation into a dedicated transport workflow page while keeping the real backend chain unchanged.

Required changes:
1. Preserve existing page actions:
   - refresh
   - select warp
   - confirm warp with popup
2. Replace the current generic section-style layout with a workflow-first layout:
   - narrow left navigation rail
   - middle warp list
   - right detail/status/action panel
   - one dominant `确认传送` action
3. Do not expand into home/back/spawn/rtp/tpa in this phase.
4. Do not add admin warp management in this phase.
5. Do not touch market or bank behavior.
6. Keep the fix local to `SERVER_TOOLS` page structure and shell integration.

Validation:
- Run `git diff --check`.
- Run targeted tests for:
  - `TerminalServiceTest`
  - `TerminalHomeScreenModelTest`
  - `TerminalPageTest`
  - `ClusterTeleportServiceTest`
  - `PlayerArrivalRestoreServiceTest`
  - ServerTools command tests if touched
- If container Gradle is blocked by environment/cache issues, record the exact blocker and do not claim tests passed.
```

## 2026-06-06 Close Round

Completed in this round:

- Added read-only recent transfer status lines to the ServerTools terminal page.
- Extended `TeleportTicketRepository` with `findRecentForPlayer(...)` and wired it into the JDBC repository.
- Added a runtime bridge seam for the default ServerTools terminal facade.
- Added targeted tests proving the default facade covers:
  - local completion feedback
  - remote dispatch feedback
  - unavailable runtime feedback
  - backend exception feedback

Targeted verification completed:

- `./gradlew compileTestJava --project-cache-dir /codex-project-cache --no-daemon --no-configuration-cache -PforceToolchainVersion=17`
- `./gradlew test --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModelTest --tests com.jsirgalaxybase.terminal.ui.TerminalPageTest --tests com.jsirgalaxybase.modules.cluster.application.ClusterTeleportServiceTest --tests com.jsirgalaxybase.modules.cluster.application.PlayerArrivalRestoreServiceTest --project-cache-dir /codex-project-cache --no-daemon --no-configuration-cache -PforceToolchainVersion=17`
- `./gradlew test --tests com.jsirgalaxybase.terminal.client.screen.TerminalHomeScreenLayoutTest --tests com.jsirgalaxybase.terminal.client.component.TerminalShellPanelsScrollTest --project-cache-dir /codex-project-cache --no-daemon --no-configuration-cache -PforceToolchainVersion=17`

Remaining acceptance work is now limited to real in-game validation and any UI density tweaks discovered there.
