package com.fatul.dyeablepoplar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

public final class DyeablePoplar implements ModInitializer {
	public static final String MOD_ID = "dyeable_poplar";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("dyeable_poplar"));

	@Override
	public void onInitialize() {
		DyedPoplarParticles.register();
		DyedPoplarBlocks.register();
		DyedPoplarBoats.register();
		PoplarDyeing.register();
		registerCreativeTab();
		LOGGER.info("Registered {} dyed poplar blocks and {} boats", DyedPoplarBlocks.count(), DyedPoplarBlocks.COLOR_ORDER.size() * 2);
	}

	/** One tab with everything, grouped by block type, each in the 16 dye colours. */
	private static void registerCreativeTab() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.dyeable_poplar"))
				.icon(() -> new ItemStack(DyedPoplarBlocks.get(DyedPoplarBlocks.Shape.PLANKS, DyeColor.RED)))
				.displayItems((parameters, output) -> {
					for (DyedPoplarBlocks.Shape shape : DyedPoplarBlocks.Shape.values()) {
						if (shape.hasItem()) {
							for (DyeColor color : DyedPoplarBlocks.COLOR_ORDER) {
								output.accept(DyedPoplarBlocks.get(shape, color));
							}
						}
					}

					for (DyeColor color : DyedPoplarBlocks.COLOR_ORDER) {
						output.accept(DyedPoplarBoats.boatItem(color));
					}

					for (DyeColor color : DyedPoplarBlocks.COLOR_ORDER) {
						output.accept(DyedPoplarBoats.chestBoatItem(color));
					}
				})
				.build());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
