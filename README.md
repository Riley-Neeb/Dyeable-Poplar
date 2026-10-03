# Dyeable Poplar

A Fabric mod for Minecraft 26.3. You can dye the whole poplar wood set in all 16 dye colors:

- logs and wood
- stripped logs and stripped wood
- planks, stairs, slabs, fences and fence gates
- doors, trapdoors, pressure plates and buttons
- signs, hanging signs and shelves
- boats and boats with chests

It also adds campfires that burn with smoke in their dye color.

Each group of blocks takes dye differently:

- **Logs and wood** get a partial tint. The bark's natural brown still shows through the dye, so a red log reads as reddish bark rather than a red block.
- **Stripped logs, stripped wood, hanging signs and shelves** get a soft pastel tint. Vanilla makes these from stripped logs.
- **Planks, stairs, slabs, fences, gates, doors, trapdoors, plates, buttons, signs and boats** are recolored fully. Vanilla makes these from planks.
- **Metal and other materials stay as they are.** That covers the chains on hanging signs, the chest on a boat with a chest, and the fire and embers of a campfire.

## Dyeing

- **Bulk crafting:** put 8 items of one kind around a dye to get 8 dyed ones. This works the same way as stained glass. Boats don't have this recipe because they don't stack.
- **Single crafting:** put a dye and one item in the crafting grid to get 1 dyed one. This works the same way as wool.
- **In the world:** right-click a placed block with a dye. The block keeps its facing, log axis, slab half, open or closed state, fence connections and waterlogging. Doors change both halves at once. Each click uses one dye, or none in Creative.
- **Re-dyeing:** all three methods also work on things that are already dyed.

Some blocks can only be dyed by crafting, because right-clicking them with a dye already does something in vanilla:

- **Signs and hanging signs:** dye colors the text.
- **Shelves:** a dye gets placed on the shelf.
- **Campfires with food cooking on them:** the food would fall off. Take the food off first.

## Colored campfire smoke

There are two ways to get a dyed campfire:

- **Craft it** like a normal campfire, with any dyed logs, wood or stripped logs of one color as the bottom row.
- **Right-click** an existing campfire with a dye.

The smoke column takes the dye's color, including the tall signal smoke you get with a hay bale underneath. The logs in the campfire get the bark tint. Cooking, putting the fire out and relighting it all work as in vanilla. A broken dyed campfire drops charcoal, or the campfire itself with Silk Touch.

## Crafting with dyed blocks

The usual poplar recipes work in dyed form, and each one keeps the color:

- **Stripping:** an axe strips a dyed log or dyed wood into the stripped block of the same color.
- **From logs:** 4 dyed logs make 3 dyed wood, and any dyed log or wood saws into 4 dyed planks.
- **From planks:** dyed planks make stairs, slabs, fences, fence gates, doors, trapdoors, pressure plates, buttons, signs and boats.
- **From stripped logs:** dyed stripped logs make hanging signs and shelves.
- **Chest boats:** a dyed boat plus a chest makes a dyed boat with a chest.

Dyed blocks count as their vanilla counterparts everywhere the game checks:

- Dyed logs smelt into charcoal, build campfires and stop nearby leaves from decaying.
- Dyed planks make sticks, crafting tables and tools.
- Dyed fences connect to other wooden fences, and villagers open dyed doors.
- Dyed blocks burn as furnace fuel, catch fire like their vanilla versions, and are mined fastest with an axe.

Everything is listed in its own **Dyeable Poplar** creative tab.

### Known quirk

The sign editing screen shows the plain poplar sign background. The placed sign itself is dyed.

## Install

1. Install Fabric Loader 0.19.5 or newer for Minecraft 26.3.
2. Install Fabric API.
3. Put `dyeable-poplar-1.0.0.jar` in your `mods` folder.

## Build

You need JDK 25. On Windows, `build.bat` downloads one into the project folder for you.

```
./gradlew build
```

The jar is written to `build/libs/`.

`./gradlew runClientGameTest` opens the game, builds a gallery of every dyed block, campfire and boat in a new world, and saves screenshots to `build/run/clientGameTest/screenshots`.

## How the textures work

The mod doesn't ship any dyed texture images. When Minecraft loads, it builds the dyed block and item textures from the vanilla ones, using `paletted_permutations` atlas sources defined in `assets/minecraft/atlases/blocks.json` and `items.json`. Each wood color in a vanilla texture is swapped for a dyed shade.

Boats are drawn from entity textures, which aren't part of an atlas. For those, the mod recolors the vanilla poplar boat textures itself when resources load, using the same palettes.

This approach has one side effect. If a resource pack repaints poplar with different colors, any pixel whose color isn't in the vanilla palette stays undyed.

To change the colors:

1. Edit the three styles (`planks_style`, `bark_style`, `pastel_style`) or the `SMOKE` colors in `tools/generate_resources.py`.
2. Also update `DyedPoplarParticles.smokeColor` in the Java code, which holds a copy of the smoke colors.
3. Run `python3 tools/generate_resources.py`. It needs Pillow.

The script regenerates every palette, model, blockstate, recipe, loot table and tag.
