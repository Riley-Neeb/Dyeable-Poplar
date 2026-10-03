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

## How the textures work

The mod doesn't ship any dyed texture images. When Minecraft loads, it builds the dyed block and item textures from the vanilla ones, using `paletted_permutations` atlas sources defined in `assets/minecraft/atlases/blocks.json` and `items.json`. Each wood color in a vanilla texture is swapped for a dyed shade.

Boats are drawn from entity textures, which aren't part of an atlas. For those, the mod recolors the vanilla poplar boat textures itself when resources load, using the same palettes.

This approach has one side effect. If a resource pack repaints poplar with different colors, any pixel whose color isn't in the vanilla palette stays undyed.

To change the colors:

1. Edit the three styles (`planks_style`, `bark_style`, `pastel_style`) or the `SMOKE` colors in `tools/generate_resources.py`.
2. Also update `DyedPoplarParticles.smokeColor` in the Java code, which holds a copy of the smoke colors.
3. Run `python3 tools/generate_resources.py`. It needs Pillow.

The script regenerates every palette, model, blockstate, recipe, loot table and tag.
