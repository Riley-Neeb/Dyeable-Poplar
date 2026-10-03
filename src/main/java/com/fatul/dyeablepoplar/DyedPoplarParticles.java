package com.fatul.dyeablepoplar;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;

/** Coloured campfire smoke: a normal and a signal-fire version for each dye. */
public final class DyedPoplarParticles {
	private static final Map<DyeColor, SimpleParticleType> COSY = new EnumMap<>(DyeColor.class);
	private static final Map<DyeColor, SimpleParticleType> SIGNAL = new EnumMap<>(DyeColor.class);

	private DyedPoplarParticles() {
	}

	static void register() {
		for (DyeColor color : DyedPoplarBlocks.COLOR_ORDER) {
			// "always spawn", like vanilla campfire smoke, so it shows at any particle setting
			COSY.put(color, Registry.register(BuiltInRegistries.PARTICLE_TYPE,
					DyeablePoplar.id(color.getSerializedName() + "_campfire_cosy_smoke"), FabricParticleTypes.simple(true)));
			SIGNAL.put(color, Registry.register(BuiltInRegistries.PARTICLE_TYPE,
					DyeablePoplar.id(color.getSerializedName() + "_campfire_signal_smoke"), FabricParticleTypes.simple(true)));
		}
	}

	public static SimpleParticleType cosySmoke(DyeColor color) {
		return COSY.get(color);
	}

	public static SimpleParticleType signalSmoke(DyeColor color) {
		return SIGNAL.get(color);
	}

	/** RGB tint for each dye's smoke. The smoke sprites are light grey, so this multiplies them. */
	public static int smokeColor(DyeColor color) {
		return switch (color) {
			case WHITE -> 0xF2F2F2;
			case LIGHT_GRAY -> 0xA9A9A2;
			case GRAY -> 0x5B6167;
			case BLACK -> 0x1D1D23;
			case BROWN -> 0x8C5A35;
			case RED -> 0xC4322B;
			case ORANGE -> 0xFF8C2B;
			case YELLOW -> 0xFFD942;
			case LIME -> 0x8CDD2D;
			case GREEN -> 0x6B8C2B;
			case CYAN -> 0x2BA9B1;
			case LIGHT_BLUE -> 0x5CC8F2;
			case BLUE -> 0x4B51C9;
			case PURPLE -> 0x9B42D1;
			case MAGENTA -> 0xE05CD4;
			case PINK -> 0xFFA9C5;
		};
	}
}
