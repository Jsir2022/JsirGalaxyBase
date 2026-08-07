# Base Vault Audited Sort v1

## Decision

Base Vault sorting is a JGB server operation, not a compatibility registration
for Inventory Bogo Sorter. The installed sorter may manage normal player
inventories, but it must not write Base Vault slots.

## Why

Inventory Bogo Sorter's server packet mutates `openContainer` slots directly.
That bypasses `BaseVaultContainer`'s versioned database commit and recovery
log. A persistent cross-server Vault cannot trust client-selected sorting rules
or allow an unlogged slot rewrite.

## Contract

- The personal Vault header's `S` button sends one server request only when the
  player's open container is `BaseVaultContainer`.
- The server locks the target account, groups identical full-NBT stacks,
  merges only to the vanilla stack limit, and orders groups by registry name,
  metadata and canonical NBT identity.
- Player inventory, cursor stack, drops and external inventories are never
  touched. Sorting is rejected while the cursor holds a stack. A fixed Vault
  that cannot represent its sorted contents fails wholly.
- Changed slots and complete before/after ItemStack NBT snapshots are written
  to `warehouse_operation_slot_change` under a single `VAULT_SORT` operation.
- The policy is fixed as `registry-meta-nbt-v1`; client sort rules do not affect
  an audited account store.

The same service operation supports `PERSONAL` (27 slots), `ENTERPRISE` (54
slots), and `PUBLIC` (54 slots). Enterprise and public calls are deliberately
service-only for now: their container opening must first have a real role and
authority check, so this change does not create a player-accessible bypass.

## Recovery

The operation is database-only and runs in one shared JDBC transaction. Any
version conflict or SQL failure rolls back the full sort. There is no physical
item delivery to replay or automatically distribute.

## Delivery Scope

This change adds the deterministic server sort primitive, the audit migration,
and the Base Vault header control. It intentionally does not sort the player
inventory, import Inventory Bogo Sorter rules, or add an unaudited compatibility
hook. The player inventory remains a normal modpack inventory managed by its
existing client features.

## Validation

- Static validation: `git diff --check`.
- Targeted tests: `BaseVaultServiceTest` and `VaultSortPlannerTest`.
- Docker Gradle compilation and the targeted tests run through the repository's
  isolated Gradle cache path. The wrapper script accepts one Gradle task only;
  tests with `--tests` filters use the same Docker/cache arguments directly.
- Deployment follow-up: migration `20260807_001_add_base_vault_sort_audit.sql` and the runtime jar were deployed to Lobby, S2 and client. Lobby is running; S2 deployment cannot complete runtime validation until its independently corrupt world metadata is restored.
