package com.fatul.dyeablepoplar;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

import net.fabricmc.fabric.api.item.v1.BlockTransformerHelper;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;

/**
 * Registers the dyed poplar blocks (16 colours of every block in the poplar set, plus campfires)
 * and knows how to swap between them.
 */
public final class DyedPoplarBlocks {
	/** The colour order vanilla uses in the Colored Blocks creative tab. */
	public static final List<DyeColor> COLOR_ORDER = List.of(
			DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK,
			DyeColor.BROWN, DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW,
			DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN, DyeColor.LIGHT_BLUE,
			DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK);

	/** Every dyeable block, in the order vanilla lists the poplar set. */
	public enum Shape {
		LOG("poplar_log", Blocks.POPLAR_LOG, true, true, 5, 5),
		WOOD("poplar_wood", Blocks.POPLAR_WOOD, true, true, 5, 5),
		STRIPPED_LOG("stripped_poplar_log", Blocks.STRIPPED_POPLAR_LOG, true, true, 5, 5),
		STRIPPED_WOOD("stripped_poplar_wood", Blocks.STRIPPED_POPLAR_WOOD, true, true, 5, 5),
		PLANKS("poplar_planks", Blocks.POPLAR_PLANKS, true, true, 5, 20),
		STAIRS("poplar_stairs", Blocks.POPLAR_STAIRS, true, true, 5, 20),
		SLAB("poplar_slab", Blocks.POPLAR_SLAB, true, true, 5, 20),
		FENCE("poplar_fence", Blocks.POPLAR_FENCE, true, true, 5, 20),
		FENCE_GATE("poplar_fence_gate", Blocks.POPLAR_FENCE_GATE, true, true, 5, 20),
		DOOR("poplar_door", Blocks.POPLAR_DOOR, true, true, 0, 0),
		TRAPDOOR("poplar_trapdoor", Blocks.POPLAR_TRAPDOOR, true, true, 0, 0),
		PRESSURE_PLATE("poplar_pressure_plate", Blocks.POPLAR_PRESSURE_PLATE, true, true, 0, 0),
		BUTTON("poplar_button", Blocks.POPLAR_BUTTON, true, true, 0, 0),
		// Right-clicking these with dye already means something in vanilla (it colours sign
		// text, or puts the dye on the shelf), so they are only dyed by crafting.
		SIGN("poplar_sign", Blocks.POPLAR_SIGN, true, false, 0, 0),
		WALL_SIGN("poplar_wall_sign", Blocks.POPLAR_WALL_SIGN, false, false, 0, 0),
		HANGING_SIGN("poplar_hanging_sign", Blocks.POPLAR_HANGING_SIGN, true, false, 0, 0),
		WALL_HANGING_SIGN("poplar_wall_hanging_sign", Blocks.POPLAR_WALL_HANGING_SIGN, false, false, 0, 0),
		SHELF("poplar_shelf", Blocks.POPLAR_SHELF, true, false, 0, 0),
		CAMPFIRE("campfire", Blocks.CAMPFIRE, true, true, 0, 0);

		private final String vanillaId;
		private final Block natural;
		private final boolean hasItem;
		private final boolean dyeableInWorld;
		/** Fire values used if vanilla's own can't be read: how readily it catches, and how fast it burns. */
		private final int igniteOdds;
		private final int burnOdds;

		Shape(String vanillaId, Block natural, boolean hasItem, boolean dyeableInWorld, int igniteOdds, int burnOdds) {
			this.vanillaId = vanillaId;
			this.natural = natural;
			this.hasItem = hasItem;
			this.dyeableInWorld = dyeableInWorld;
			this.igniteOdds = igniteOdds;
			this.burnOdds = burnOdds;
		}

		public Block natural() {
			return natural;
		}

		public boolean hasItem() {
			return hasItem;
		}
	}

	private static final Map<Shape, Map<DyeColor, Block>> DYED = new EnumMap<>(Shape.class);
	/** Shape of every block this mod can recolour, undyed vanilla ones included. */
	private static final Map<Block, Shape> SHAPE_OF = new HashMap<>();
	/** Colour of each dyed block; undyed vanilla blocks are absent. */
	private static final Map<Block, DyeColor> COLOR_OF = new HashMap<>();

	private DyedPoplarBlocks() {
	}

	static void register() {
		for (Shape shape : Shape.values()) {
			DYED.put(shape, new EnumMap<>(DyeColor.class));
			SHAPE_OF.put(shape.natural, shape);
		}

		for (DyeColor color : COLOR_ORDER) {
			register(color, Shape.LOG, RotatedPillarBlock::new);
			register(color, Shape.WOOD, RotatedPillarBlock::new);
			register(color, Shape.STRIPPED_LOG, RotatedPillarBlock::new);
			register(color, Shape.STRIPPED_WOOD, RotatedPillarBlock::new);
			Block planks = register(color, Shape.PLANKS, Block::new);
			register(color, Shape.STAIRS, properties -> new StairBlock(planks.defaultBlockState(), properties));
			register(color, Shape.SLAB, SlabBlock::new);
			register(color, Shape.FENCE, FenceBlock::new);
			register(color, Shape.FENCE_GATE, properties -> new FenceGateBlock(WoodType.POPLAR, properties));
			register(color, Shape.DOOR, properties -> new DoorBlock(BlockSetType.POPLAR, properties));
			register(color, Shape.TRAPDOOR, properties -> new TrapDoorBlock(BlockSetType.POPLAR, properties));
			register(color, Shape.PRESSURE_PLATE, properties -> new PressurePlateBlock(BlockSetType.POPLAR, properties));
			register(color, Shape.BUTTON, properties -> new ButtonBlock(BlockSetType.POPLAR, 30, properties));
			registerSigns(color);
			Block shelf = register(color, Shape.SHELF, ShelfBlock::new);
			BlockEntityTypes.SHELF.addValidBlock(shelf);
			Block campfire = register(color, Shape.CAMPFIRE, properties -> new DyedCampfireBlock(color, properties));
			BlockEntityTypes.CAMPFIRE.addValidBlock(campfire);

			// An axe strips a dyed log or wood into the stripped block of the same colour.
			BlockTransformerHelper.registerStripping(get(Shape.LOG, color), get(Shape.STRIPPED_LOG, color));
			BlockTransformerHelper.registerStripping(get(Shape.WOOD, color), get(Shape.STRIPPED_WOOD, color));
		}

		// Same fire behaviour as the matching vanilla block.
		FlammableBlockRegistry flammables = FlammableBlockRegistry.getDefaultInstance();

		for (Shape shape : Shape.values()) {
			FlammableBlockRegistry.Entry vanilla = flammables.get(shape.natural);
			boolean vanillaKnown = vanilla != null && (vanilla.getIgniteOdds() > 0 || vanilla.getBurnOdds() > 0);

			for (Block block : DYED.get(shape).values()) {
				if (vanillaKnown) {
					flammables.add(block, vanilla);
				} else if (shape.igniteOdds > 0) {
					flammables.add(block, shape.igniteOdds, shape.burnOdds);
				}
			}
		}

		// Burn in furnaces exactly as long as the matching vanilla item.
		DefaultItemComponentEvents.MODIFY.register(DyedPoplarBlocks::copyVanillaFuel);
	}

	private static void registerSigns(DyeColor color) {
		Block sign = registerBlock(color, Shape.SIGN, properties -> new StandingSignBlock(WoodType.POPLAR, properties),
				BlockBehaviour.Properties.ofFullCopy(Blocks.POPLAR_SIGN));
		Block wallSign = registerBlock(color, Shape.WALL_SIGN, properties -> new WallSignBlock(WoodType.POPLAR, properties),
				BlockBehaviour.Properties.ofFullCopy(Blocks.POPLAR_WALL_SIGN)
						.overrideLootTable(sign.getLootTable())
						.overrideDescription(sign.getDescriptionId()));
		registerItem(color, Shape.SIGN, key -> new StandingAndWallBlockItem(sign, wallSign, Direction.DOWN,
				new Item.Properties().stacksTo(16).signText().useBlockDescriptionPrefix().setId(key)));
		BlockEntityTypes.SIGN.addValidBlock(sign);
		BlockEntityTypes.SIGN.addValidBlock(wallSign);

		Block hangingSign = registerBlock(color, Shape.HANGING_SIGN,
				properties -> new CeilingHangingSignBlock(WoodType.POPLAR, properties),
				BlockBehaviour.Properties.ofFullCopy(Blocks.POPLAR_HANGING_SIGN));
		Block wallHangingSign = registerBlock(color, Shape.WALL_HANGING_SIGN,
				properties -> new WallHangingSignBlock(WoodType.POPLAR, properties),
				BlockBehaviour.Properties.ofFullCopy(Blocks.POPLAR_WALL_HANGING_SIGN)
						.overrideLootTable(hangingSign.getLootTable())
						.overrideDescription(hangingSign.getDescriptionId()));
		registerItem(color, Shape.HANGING_SIGN, key -> new HangingSignItem(hangingSign, wallHangingSign,
				new Item.Properties().stacksTo(16).signText().useBlockDescriptionPrefix().setId(key)));
		BlockEntityTypes.HANGING_SIGN.addValidBlock(hangingSign);
		BlockEntityTypes.HANGING_SIGN.addValidBlock(wallHangingSign);
	}

	/** Registers a block plus its ordinary block item. */
	private static Block register(DyeColor color, Shape shape, Function<BlockBehaviour.Properties, Block> factory) {
		Block block = registerBlock(color, shape, factory, BlockBehaviour.Properties.ofFullCopy(shape.natural));
		registerItem(color, shape, key -> new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(key)));
		return block;
	}

	private static Block registerBlock(DyeColor color, Shape shape, Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(color, shape));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key,
				factory.apply(properties.mapColor(color.getMapColor()).setId(key)));
		DYED.get(shape).put(color, block);
		SHAPE_OF.put(block, shape);
		COLOR_OF.put(block, color);
		return block;
	}

	private static void registerItem(DyeColor color, Shape shape, Function<ResourceKey<Item>, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(color, shape));
		Registry.register(BuiltInRegistries.ITEM, key, factory.apply(key));
	}

	private static Identifier id(DyeColor color, Shape shape) {
		return DyeablePoplar.id(color.getSerializedName() + "_dyed_" + shape.vanillaId);
	}

	private static void copyVanillaFuel(DefaultItemComponentEvents.ModifyContext context) {
		for (Shape shape : Shape.values()) {
			if (!shape.hasItem) {
				continue;
			}

			CookingFuel fuel = shape.natural.asItem().components().get(DataComponents.COOKING_FUEL);

			if (fuel != null) {
				List<Item> items = new ArrayList<>();
				DYED.get(shape).values().forEach(block -> items.add(block.asItem()));
				context.modify(items, (builder, item) -> builder.set(DataComponents.COOKING_FUEL, fuel));
			}
		}
	}

	/**
	 * The block that {@code block} turns into when dyed {@code dye} in the world, or {@code null}
	 * when it can't be dyed in place or is already that colour.
	 */
	public static Block recolored(Block block, DyeColor dye) {
		Shape shape = SHAPE_OF.get(block);

		if (shape == null || !shape.dyeableInWorld || COLOR_OF.get(block) == dye) {
			return null;
		}

		return get(shape, dye);
	}

	public static Block get(Shape shape, DyeColor color) {
		return DYED.get(shape).get(color);
	}

	public static int count() {
		return COLOR_OF.size();
	}
}
