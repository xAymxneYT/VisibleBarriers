# Visible Barriers

By xAymxneYT.

Configurable barrier visibility for Minecraft 1.8.9 on Ornithe Gen2, with native
OneConfig settings and connected surfaces.

## Install

Requires Fabric Loader 0.19.5+, OneConfig 1.7.1+ and Java 21+. OneClient 1.8.9
supplies these dependencies. Remove the previous Barrier jar before installing
`visible-barriers-0.5.0+1.8.9-ornithe-gen2.jar` in the instance's `mods` folder.

Open **OneConfig → Visible Barriers** or press **B** to toggle visibility. Check the
server's rules before using visible barriers.

## Settings

General contains the enable switch, connected textures, outline, fill, marker
type and colour pickers. Outline width sits below outline colour; marker size
sits below marker type and controls floating markers. Control is the last
section. Range, fade and performance limits are in Advanced.

| Setting | Default |
| --- | --- |
| Outline | `E5560000`, approximately 90% opacity |
| Fill | `66FF0000`, 40% opacity |
| Width | 4 pixels; adjustable from 1 to 10 |
| Marker type | 2D block texture |
| Connected textures | On |
| Distance fade | On |
| Toggle key | B |

Chroma is available inside the colour pickers. Minecraft's selection outline is
unchanged. The supplied icon is included in both OneConfig and the mod metadata.

Connected mode removes internal faces, merges flat surfaces and joins outline
segments. Only camera-facing surfaces and outline edges are drawn. Native OpenGL lines
avoid per-frame matrix readbacks; the graphics driver may limit line width.
Unchanged geometry is reused when moving around. Internal faces are removed
even with connected textures off, and shared grid edges are drawn once.
Minecraft's floating barrier particles are suppressed while the mod is active
to prevent duplicate signs when holding a barrier item.

Loaded chunks are discovered with incremental scans. Placing or breaking a
barrier updates the cache directly and refreshes the geometry on the next
rendered frame. Interactive edits in the same frame are combined into one
rebuild; bulk discovery builds remain on the background worker. Stale background
results are discarded when an edit arrives.

The cache is capped at 65,536 barriers; the nearest 2,048 are shown by default.
Range and block limits may cut a large structure into a smaller visible region.
Initial discovery of loaded chunks can still have a short scan delay.

Barrier visuals render before entities, so they do not paint over players and
armour in third-person view. The corrected SDL B binding replaces the previous
incorrect F9 default once on upgrade. Other custom bindings are preserved.

## Resource packs

Both marker types use `minecraft:textures/items/barrier.png`. Connected block
textures tile once per block. This does not load OptiFine CTM rules.

For a separate image, put `assets/barrier/textures/marker.png` in a resource pack
and enable Custom texture under Advanced. A missing image uses Minecraft's
missing-texture pattern.

## Build

Use a JDK 21+:

```sh
./gradlew build
```

On Windows use `gradlew.bat build`. The remapped mod jar is in `build/libs/`.
This is an Ornithe Gen2 build, not Forge or Legacy Fabric.

Tests cover mesh coverage, connected edges, negative coordinates, cavities,
section planning, live edits, keybind migration, fading and camera-facing surfaces. Visual changes still require
an in-game check with the target mod list and graphics driver.

## License

MIT. See LICENSE.
