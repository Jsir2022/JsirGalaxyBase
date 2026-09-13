# Third-Party Notices

## BetterQuesting

Galaxy Quest is designed to import GTNH BetterQuesting definitions and preserve its mature quest, task, reward,
editing and detection semantics while replacing the GUI, networking and file-backed runtime with Base-owned
components and PostgreSQL authority.

- Project: `GTNewHorizons/BetterQuesting`
- Reference version: `3.7.15-GTNH`
- Reference commit: `524b365211b6b3a9672cab8ae45b4e07e726d49e`
- License: MIT

The source inventory and adaptation boundary are packaged as `META-INF/betterquesting-provenance.md`; the MIT
license is packaged as `META-INF/licenses/BetterQuesting-MIT.txt`. Base does not declare a BetterQuesting runtime
dependency for the new quest core.

## GTNH ServerUtilities

Portions of the personal land domain and chunk-protection implementation are derived from or informed by
GTNH ServerUtilities.

- Project: `GTNewHorizons/ServerUtilities`
- Reference source: `Reference/ServerUtilities`
- GTNH modifications copyright: 2021-2024 The GTNH Team
- Original FTB Utilities / FTB Library code copyright: 2016 LatvianModder
- License for GTNH changes: GNU Lesser General Public License v3.0 or later
- Original code license: MIT

The complete LGPL license text is kept at `Reference/ServerUtilities/LICENSE.txt` and is copied into the
published JAR under `META-INF/licenses/ServerUtilities-LGPL-3.0-or-later.txt`.

Derived source files retain an SPDX and provenance header. JsirGalaxyBase-specific code remains under the
repository's MIT license except where a file is explicitly identified as derived from ServerUtilities.

## Applied Energistics 2 Unofficial

The Warehouse Drive Phase 0 adapter derives its Cell, channel, power and simulation integration shape from
Applied Energistics 2 Unofficial's `TileDrive` and Cell API.

- Project: `GTNewHorizons/Applied-Energistics-2-Unofficial`
- Reference source: `Reference/Applied-Energistics-2-Unofficial`
- License: GNU Lesser General Public License v3.0 or later

The project compiles against the GTNH-pinned AE2 API as `compileOnly`; it does not bundle AE2. The LGPL text is copied
into the published JAR under `META-INF/licenses/Applied-Energistics-2-LGPL-3.0-or-later.txt`. Derived adapters retain
an SPDX and provenance header.
## Qz-UILib algorithm provenance

Galaxy UI 2 contains narrowly internalized and renamed algorithms derived from or informed by fixed Qz-UILib
revisions. It does not bundle, link to, or call the Qz-UILib Mod.

- Project: `QuanhuZeYu/Qz-UILib`
- 1.8.2 commit: `bbc7f1380c02faa7e13b227d01f09b35bee639ed` (MIT)
- 4.1.3-LTS commit: `c1c9874ca20d2f1526f3ce3769dcb704e46e4258` (LGPL-3.0-or-later)
- 4.7.0 commit: `23569af8d4de073fc16cdeb817c86945fa78bd7f` (LGPL-3.0-or-later)
- 4.8.0 commit: `7937cd042910c8182d899d41aa5692d1c8b4a98a` (LGPL-3.0-or-later)

The actual derived-file inventory and adaptation notes are packaged at
`META-INF/qz-uilib-provenance.md`. The MIT and LGPL texts are packaged under `META-INF/licenses/`.
Derived Java files retain an SPDX and upstream revision header. No Qz DOM/CSS, Mixin, network, configuration,
HUD, global font hook, Mod bootstrap, package, or runtime dependency is included.
