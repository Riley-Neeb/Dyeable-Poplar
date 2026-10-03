package com.fatul.dyeablepoplar;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.DispenserBlock;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

/** Dyed poplar boats and boats with chests: an entity type and an item for each colour. */
public final class DyedPoplarBoats {
	private static final Map<DyeColor, EntityType<Boat>> BOATS = new EnumMap<>(DyeColor.class);
	private static final Map<DyeColor, EntityType<ChestBoat>> CHEST_BOATS = new EnumMap<>(DyeColor.class);
	private static final Map<DyeColor, Item> BOAT_ITEMS = new EnumMap<>(DyeColor.class);
	private static final Map<DyeColor, Item> CHEST_BOAT_ITEMS = new EnumMap<>(DyeColor.class);

	private DyedPoplarBoats() {
	}

	static void register() {
		for (DyeColor color : DyedPoplarBlocks.COLOR_ORDER) {
			String name = color.getSerializedName() + "_dyed_poplar";

			EntityType<Boat> boat = entity(name + "_boat", (type, level) -> new Boat(type, level, () -> BOAT_ITEMS.get(color)));
			EntityType<ChestBoat> chestBoat = entity(name + "_chest_boat",
					(type, level) -> new ChestBoat(type, level, () -> CHEST_BOAT_ITEMS.get(color)));
			BOATS.put(color, boat);
			CHEST_BOATS.put(color, chestBoat);
			BOAT_ITEMS.put(color, item(name + "_boat", boat));
			CHEST_BOAT_ITEMS.put(color, item(name + "_chest_boat", chestBoat));
		}

		DefaultItemComponentEvents.MODIFY.register(context -> {
			copyFuel(context, Items.POPLAR_BOAT, BOAT_ITEMS);
			copyFuel(context, Items.POPLAR_CHEST_BOAT, CHEST_BOAT_ITEMS);
		});
	}

	private static <T extends Entity> EntityType<T> entity(String path, EntityType.EntityFactory<T> factory) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, DyeablePoplar.id(path));
		// the same size and settings as the vanilla boats
		EntityType<T> type = EntityType.Builder.of(factory, MobCategory.MISC)
				.noLootTable()
				.sized(1.375F, 0.5625F)
				.eyeHeight(0.5625F)
				.clientTrackingRange(10)
				.build(key);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
	}

	private static Item item(String path, EntityType<? extends AbstractBoat> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, DyeablePoplar.id(path));
		BoatItem item = Registry.register(BuiltInRegistries.ITEM, key, new BoatItem(type, new Item.Properties().stacksTo(1).setId(key)));
		DispenserBlock.registerBehavior(item, new BoatDispenseItemBehavior(type));
		return item;
	}

	private static void copyFuel(DefaultItemComponentEvents.ModifyContext context, Item vanilla, Map<DyeColor, Item> dyed) {
		CookingFuel fuel = vanilla.components().get(DataComponents.COOKING_FUEL);

		if (fuel != null) {
			context.modify(dyed.values(), (builder, item) -> builder.set(DataComponents.COOKING_FUEL, fuel));
		}
	}

	public static EntityType<Boat> boat(DyeColor color) {
		return BOATS.get(color);
	}

	public static EntityType<ChestBoat> chestBoat(DyeColor color) {
		return CHEST_BOATS.get(color);
	}

	public static Item boatItem(DyeColor color) {
		return BOAT_ITEMS.get(color);
	}

	public static Item chestBoatItem(DyeColor color) {
		return CHEST_BOAT_ITEMS.get(color);
	}
}
