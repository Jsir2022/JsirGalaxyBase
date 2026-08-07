# Personal Base Vault Market Integration v1

## Scope

The personal Base Vault is the only item source and item delivery account for the current market release.
The Minecraft player inventory is only used by the separate Vault container page. Market pages must never
fall back to the held item or player inventory when a Vault selection is absent or invalid.

## Asset Boundary

| Workflow | Source | Destination | Notes |
| --- | --- | --- | --- |
| Standardized deposit | Personal Base Vault, aggregated by catalog product | Market `AVAILABLE` custody | One request id covers both legs. |
| Standardized purchase | Bank balance | `CLAIMABLE` | Claim delivers only to personal Base Vault. |
| Custom publish | One exact personal Vault slot | Custom listing escrow | The selected stack must contain exactly one item. |
| Custom cancel or claim | Custom delivery escrow | Personal Base Vault | Full Vault keeps delivery pending. |
| Exchange | One exact personal Vault slot | Bank STARCOIN settlement | The server validates the selected task-coin stack and formal quote. |

## Read-only Picker

All market pages use the same read-only 9x3 personal Vault picker. Opening, searching, filtering, or
closing it never changes any Vault slot. Standardized deposit chooses a catalog product and explicit
quantity, safely aggregated across matching Vault slots. Custom publishing and exchange select one
exact slot. The server remains authoritative for slot version, capacity, catalog admission, quote validity,
and idempotent recovery.

## Future Resolution

AE2 Cell Bay/Drive/Port and authorised enterprise/public stores are not part of v1. A future account
storage resolver may prefer an authorised entity store and fall back to Base Vault without changing these
market service contracts.
