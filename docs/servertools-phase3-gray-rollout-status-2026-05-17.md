# Server Tools / Cluster Phase 3 Gray Rollout Status

Date: 2026-05-17

This note records the actual gray-rollout preparation performed for the `servertools / cluster` phase 3 chain.

## Scope

The original phase 3 prompt expected MCSM-managed instances under `../GroupServer`. The current machine state is different:

- Runtime data is mounted from `/media/u24/data/gtnh/data`.
- The active process manager is Docker + supervisor in container `galaxy-gtnh`.
- `/media/u24/data/gtnh/data` is mounted in the container as `/gtnh/GroupServer`.
- The PostgreSQL container is `galaxy-base`.

This rollout kept the original safety boundary:

- S1 was not restarted.
- S1 mods and JsirGalaxyBase config were not modified.
- The gray chain was limited to Entrance, Lobby, and S2.

## Instance Mapping

| Role | Host path | Container path | Port |
| --- | --- | --- | --- |
| Entrance | `/media/u24/data/gtnh/data/Galaxy_GTNH_Entrance` | `/gtnh/GroupServer/Galaxy_GTNH_Entrance` | `127.0.0.1:25566` |
| Lobby | `/media/u24/data/gtnh/data/Galaxy_GTNH_Lobby` | `/gtnh/GroupServer/Galaxy_GTNH_Lobby` | `127.0.0.1:25564` |
| S2 | `/media/u24/data/gtnh/data/Galaxy_GTNH284_S2` | `/gtnh/GroupServer/Galaxy_GTNH284_S2` | `127.0.0.1:25567` |
| S1 | `/media/u24/data/gtnh/data/Galaxy_GTNH284_S1` | `/gtnh/GroupServer/Galaxy_GTNH284_S1` | `0.0.0.0:25565` |

Velocity currently routes only:

- `galaxy_gtnh_lobby = 127.0.0.1:25564`
- `galaxy_gtnh284_s2 = 127.0.0.1:25567`

No S1 backend is present in the gray proxy config.

## Artifact

The deployed artifact is:

- `build/libs/jsirgalaxybase-ed7e2cf.jar`
- SHA256: `02dcd79439cb7e8fa7896299d047d637e3d9dd3bddd7d7b54fdff8beca98e065`

The same jar was deployed to:

- `/media/u24/data/gtnh/data/Galaxy_GTNH_Lobby/mods/jsirgalaxybase-ed7e2cf.jar`
- `/media/u24/data/gtnh/data/Galaxy_GTNH284_S2/mods/jsirgalaxybase-ed7e2cf.jar`

Previous `7545ce9` gray jars were moved to each instance's `mods_disabled/` directory with a `disabled-20260517-141714` suffix.

The current artifact was rebuilt successfully inside the `galaxy-dev` compose service with:

```bash
docker compose -f /media/u24/data/gtnh/docker/projects/docker-compose.yml run --rm -e GRADLE_USER_HOME=/tmp/gradle-home galaxy-dev ./gradlew assemble --no-configuration-cache -PforceToolchainVersion=17
```

Host-local Gradle could not be used because the host environment has no Java/JDK configured.

## Configuration

Lobby and S2 already had `config/jsirgalaxybase-server.cfg` with:

- `bankingPostgresEnabled=true`
- `bankingSourceServerId=galaxy_gtnh_lobby` for Lobby
- `bankingSourceServerId=galaxy_gtnh284_s2` for S2
- JDBC endpoint targeting `galaxy-base:5432/jsirgalaxybase`

Docker compose persistent environment was updated in `/media/u24/data/gtnh/docker/projects/.env`:

- `ENABLE_ENTRANCE=true`
- `ENABLE_LOBBY=true`
- `ENABLE_S2=true`
- `LOBBY_XMS=4G`
- `LOBBY_XMX=4G`
- `S2_XMS=4G`
- `S2_XMX=4G`

`ENABLE_S1=true` was already present and was not changed.

The running container supervisor config was also updated for the current container lifetime so the gray instances could start without rebuilding the container.

## Database Preparation

The `jsirgalaxybase` database initially had no schema tables or migration history. Applying `scripts/db-migrate.sh` directly failed because the versioned migrations are incremental and expect the base DDL to already exist.

The following project-provided DDL files were applied first:

- `docs/banking-postgresql-ddl.sql`
- `docs/market-postgresql-ddl.sql`
- `docs/servertools-cluster-postgresql-ddl.sql`

Then `scripts/db-migrate.sh` was executed inside `galaxy-base` against the same database. Applied migrations are:

- `20260403_001_align_banking_ledger_entry_frozen_balances.sql`
- `20260404_001_add_custom_market_minimal_listing_chain.sql`
- `20260404_002_align_custom_market_single_item_claim_completion.sql`
- `20260411_001_add_servertools_cluster_phase1.sql`
- `20260411_002_expand_cluster_transfer_ticket_lifecycle.sql`

The PostgreSQL role password was aligned to the existing Lobby/S2 server config so network JDBC authentication succeeds. The password value is intentionally not recorded here.

The cluster server directory now contains:

- `galaxy_gtnh_lobby`
- `galaxy_gtnh284_s2`

After both gray servers started, each registered itself as a local enabled server through the current module startup path.

## Startup Result

Supervisor status after rollout:

```text
entrance  RUNNING
lobby     RUNNING
s1        RUNNING
s2        RUNNING
```

Listener state:

- Entrance listens on container port `25566`, exposed as host `127.0.0.1:25566`.
- Lobby listens on container port `25564`, exposed as host `127.0.0.1:25564`.
- S2 listens on container port `25567`, exposed as host `127.0.0.1:25567`.
- S1 remains on `25565`.

TCP probes from the host succeeded for:

- `127.0.0.1:25564`
- `127.0.0.1:25566`
- `127.0.0.1:25567`

Readiness log lines:

- Entrance: Velocity reports `Done`.
- Lobby: Minecraft reports `Done`, and JsirGalaxyBase reports banking, market, and cluster runtime prepared for `galaxy_gtnh_lobby`.
- S2: Minecraft reports `Done`, and JsirGalaxyBase reports banking, market, and cluster runtime prepared for `galaxy_gtnh284_s2`.

## Notes And Residual Risk

- The gray chain is started and observable, but no player-level cross-server teleport command was exercised in this round.
- The logs still contain GTNH/modpack noise such as client-side mixin warnings, duplicate microblock material errors, missing signatures, and update-check failures. These did not prevent startup.
- The original MCSM-specific instructions are stale for this machine state. Future operations should use the Docker + supervisor chain unless MCSM is reintroduced.
- The current `cluster_server_directory.local_server` field becomes true for both Lobby and S2 because each server upserts itself in a shared table. Current dispatch logic compares target ids directly and does not use this flag for routing, but this field may deserve cleanup in a later code-design pass.

## 2026-05-18 Follow-up Validation

The "no player-level cross-server teleport command was exercised" note above only describes the 2026-05-17 rollout round.

On 2026-05-18, the gray chain was exercised in game through the namespace warp command:

- `/jgbst warp list` returned the configured system warps.
- `/jgbst warp s2test` transferred the player from Lobby to S2 and restored the target landing point.
- `/jgbst warp lobbytest` transferred the player from S2 back to Lobby and restored the target landing point.
- The related rows in `cluster_transfer_ticket` reached `COMPLETED`.

The real test exposed timeout pressure during GTNH server switching. The transfer ticket TTL and Velocity read timeout were increased before the successful validation.
