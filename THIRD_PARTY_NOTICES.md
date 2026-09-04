# Third-Party Notices

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
