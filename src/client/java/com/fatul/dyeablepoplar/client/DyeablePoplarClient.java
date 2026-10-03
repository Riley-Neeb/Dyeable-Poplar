package com.fatul.dyeablepoplar.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.DyeColor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import com.fatul.dyeablepoplar.DyeablePoplar;
import com.fatul.dyeablepoplar.DyedPoplarBlocks;
import com.fatul.dyeablepoplar.DyedPoplarBoats;
import com.fatul.dyeablepoplar.DyedPoplarParticles;

public final class DyeablePoplarClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ParticleProviderRegistry particles = ParticleProviderRegistry.getInstance();

		for (DyeColor color : DyedPoplarBlocks.COLOR_ORDER) {
			int rgb = DyedPoplarParticles.smokeColor(color);
			particles.register(DyedPoplarParticles.cosySmoke(color), sprites -> new DyedCampfireSmokeParticle.Provider(sprites, false, rgb));
			particles.register(DyedPoplarParticles.signalSmoke(color), sprites -> new DyedCampfireSmokeParticle.Provider(sprites, true, rgb));

			registerBoat(DyedPoplarBoats.boat(color), "boat/", color, false);
			registerBoat(DyedPoplarBoats.chestBoat(color), "chest_boat/", color, true);
		}

		DyedBoatTextures.register();
	}

	/**
	 * The vanilla boat renderer takes its texture from the model layer's name, so a layer named
	 * {@code dyeable_poplar:boat/red_dyed_poplar} draws {@code textures/entity/boat/red_dyed_poplar.png},
	 * which {@link DyedBoatTextures} provides.
	 */
	private static void registerBoat(EntityType<? extends AbstractBoat> type, String folder, DyeColor color, boolean chest) {
		ModelLayerLocation layer = new ModelLayerLocation(DyeablePoplar.id(folder + color.getSerializedName() + "_dyed_poplar"), "main");
		ModelLayerRegistry.TexturedLayerDefinitionProvider model = chest ? BoatModel::createChestBoatModel : BoatModel::createBoatModel;
		ModelLayerRegistry.registerModelLayer(layer, model);
		EntityRenderers.register(type, context -> new BoatRenderer(context, layer));
	}
}
