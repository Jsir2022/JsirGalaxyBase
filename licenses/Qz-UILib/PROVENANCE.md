# Qz-UILib provenance for Galaxy UI 2

Galaxy UI 2 is self-contained and does not link to, load, or package the
`qz_uilib` Mod. This directory records the fixed upstream revisions reviewed
for a small number of internalized algorithms.

| Generation | Commit | License | Material reviewed |
| --- | --- | --- | --- |
| 1.8.2 | `bbc7f1380c02faa7e13b227d01f09b35bee639ed` | MIT | basic font, shader, and primitive rendering structure |
| 4.1.3-LTS | `c1c9874ca20d2f1526f3ce3769dcb704e46e4258` | LGPL-3.0-or-later | clipping, GL boundary, resource and Container lifecycle |
| 4.7.0 | `23569af8d4de073fc16cdeb817c86945fa78bd7f` | LGPL-3.0-or-later | resource lifecycle and dirty-scene behavior reviewed; no font code retained |
| 4.8.0 | `7937cd042910c8182d899d41aa5692d1c8b4a98a` | LGPL-3.0-or-later | rounded geometry, clip restoration, dirty scene and GL-state fixes |

## Files derived or informed by Qz 4.x

- `src/main/java/com/jsirgalaxybase/client/ui2/render/RoundedRectGeometry.java`
  derives radius clamping and adaptive tessellation from the 4.8 rounded-shape
  implementation, rewritten around UI2 `DrawCommand` geometry.
- `src/main/java/com/jsirgalaxybase/client/ui2/render/MinecraftClipStack.java`
  is informed by the 4.1/4.8 clip-stack lifecycle and is rewritten for nested
  framebuffer scissor intersection and host-state restoration.
Those files carry `SPDX-License-Identifier: LGPL-3.0-or-later` and precise
revision comments. The shape rasterization, coverage fringe, border ring,
renderer split, Minecraft `FontRenderer` adapter, UI2 Runtime, Store, hosts and components are Base-specific
implementations. No Qz package names, Mixins, network code, configuration,
DOM/CSS layer, custom font rasterizer, global font replacement, HUD, or Mod bootstrap are included.

The upstream repositories are intentionally not vendored. The packaged JAR
contains this provenance file, the MIT text above, the LGPL v3 text, and the
project-level `THIRD_PARTY_NOTICES.md`.
