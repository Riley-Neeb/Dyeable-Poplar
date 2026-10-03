package com.fatul.dyeablepoplar.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.DyeColor;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;

import com.fatul.dyeablepoplar.DyeablePoplar;
import com.fatul.dyeablepoplar.DyedPoplarBlocks;

/**
 * Boats are drawn from whole entity textures rather than from a texture atlas, so the atlas
 * palette swap that dyes everything else can't reach them. Instead, every time resources load,
 * this recolours the current vanilla poplar boat textures with the same palettes and registers
 * the results under the names the dyed boat renderers look for.
 */
final class DyedBoatTextures implements ResourceManagerReloadListener {
	static final Identifier ID = DyeablePoplar.id("dyed_boat_textures");
	private static final String PALETTES = "textures/palettes/poplar_details/";

	static void register() {
		ResourceLoader loader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
		loader.registerReloadListener(ID, new DyedBoatTextures());
		loader.addListenerOrdering(ResourceReloaderKeys.Client.TEXTURES, ID);
	}

	static Identifier textureId(String kind, DyeColor color) {
		return DyeablePoplar.id("textures/entity/" + kind + "/" + color.getSerializedName() + "_dyed_poplar.png");
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		try {
			int[] key = readPixels(manager, DyeablePoplar.id(PALETTES + "key.png")).pixels;

			for (String kind : List.of("boat", "chest_boat")) {
				Image vanilla = readPixels(manager, Identifier.withDefaultNamespace("textures/entity/" + kind + "/poplar.png"));

				for (DyeColor color : DyedPoplarBlocks.COLOR_ORDER) {
					int[] palette = readPixels(manager, DyeablePoplar.id(PALETTES + color.getSerializedName() + ".png")).pixels;
					Map<Integer, Integer> swap = new HashMap<>();

					for (int i = 0; i < Math.min(key.length, palette.length); i++) {
						swap.put(key[i] & 0xFFFFFF, palette[i] & 0xFFFFFF);
					}

					NativeImage dyed = new NativeImage(vanilla.width, vanilla.height, false);

					for (int y = 0; y < vanilla.height; y++) {
						for (int x = 0; x < vanilla.width; x++) {
							int argb = vanilla.pixels[y * vanilla.width + x];
							Integer rgb = (argb >>> 24) == 0 ? null : swap.get(argb & 0xFFFFFF);
							dyed.setPixel(x, y, rgb == null ? argb : (argb & 0xFF000000) | rgb);
						}
					}

					Identifier id = textureId(kind, color);
					Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, dyed));
				}
			}
		} catch (IOException e) {
			DyeablePoplar.LOGGER.error("Couldn't build the dyed boat textures", e);
		}
	}

	private record Image(int width, int height, int[] pixels) {
	}

	private static Image readPixels(ResourceManager manager, Identifier id) throws IOException {
		Optional<Resource> resource = manager.getResource(id);

		if (resource.isEmpty()) {
			throw new IOException("Missing texture " + id);
		}

		try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
			int width = image.getWidth();
			int height = image.getHeight();
			int[] pixels = new int[width * height];

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					pixels[y * width + x] = image.getPixel(x, y);
				}
			}

			return new Image(width, height, pixels);
		}
	}
}
