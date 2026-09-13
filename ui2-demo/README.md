# Galaxy UI 2 Lab

This module is a Java 8 / Java2D development preview for the shared `ui2-lab` document and platform-independent
`ui2-core` runtime. The same document is available through the Minecraft F8 Lab. This module does not
depend on Minecraft, Forge, LWJGL, AE2, ModularUI or the legacy JsirGalaxyBase Canvas framework.

Run the automated tests and headless exporter through the project's validated Docker Gradle environment:

```text
scripts/render-ui2-visuals.sh
```

Generated PNG, DrawList, component-tree audits and an `index.html` gallery are written under
`ui2-demo/build/ui-lab/`. They cover five scenes at `350x193`, `427x240` and `620x340`: overview,
controls, data, overlay and inventory.

The script mounts a host CJK font into the disposable development container. The font is only used by the
Java2D preview and is never copied into the Base sources, resources or runtime JAR. Override it with the
`UI2_PREVIEW_FONT` environment variable when required.

On a machine with a graphical Java environment, launch the interactive preview with:

```text
./gradlew :ui2-demo:run
```

Java2D verifies composition, density and state presentation. It is not the production renderer and cannot validate
Minecraft OpenGL state, glyph textures, native item rendering or Container/Slot behavior.

In the development client, press `F8` to open the shared Minecraft UI Lab. Use `D` for layout diagnostics,
`M` for reduced motion, and `C` on the Inventory page to enter the local Container/Slot Lab. The Container Lab
never sends unknown-window inventory actions to the server. Inside it, `Enter` opens the modal-blocking check;
all temporary cursor, hotbar and armor changes are restored on exit.
