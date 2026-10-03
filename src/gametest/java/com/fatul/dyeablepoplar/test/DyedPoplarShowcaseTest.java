package com.fatul.dyeablepoplar.test;

import java.util.List;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Builds a gallery of every dyed block, campfire and boat in a fresh creative world and takes
 * screenshots of it, so the textures, models, smoke and boat renderers can be checked by eye.
 * Run with {@code gradlew runClientGameTest}; screenshots land in build/run/clientGameTest/screenshots.
 */
public class DyedPoplarShowcaseTest implements FabricClientGameTest {
	private static final List<String> COLORS = List.of("white", "light_gray", "gray", "black", "brown", "red", "orange",
			"yellow", "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink");
	private static final int FLOOR = 200;

	/** One row per block type, in the order the screenshots group them. The camera looks from the north. */
	private static final List<String> ROWS = List.of(
			"poplar_log", "poplar_wood", "stripped_poplar_log", "stripped_poplar_wood",
			"poplar_planks", "poplar_stairs[facing=south]", "poplar_slab", "poplar_fence",
			"poplar_fence_gate[facing=north]", "poplar_door", "poplar_trapdoor[facing=north]", "poplar_pressure_plate",
			"poplar_button[face=floor,facing=north]", "poplar_sign[rotation=8]", "poplar_hanging_sign[rotation=8]",
			"poplar_shelf[facing=north]");

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();

			server.runCommand("forceload add -4 -24 34 72");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @a");
			camera(server, 15, 10, -16, 35);
			singleplayer.getConnection().waitForChunksRender();

			server.runCommand("fill -4 %d -24 34 %d 72 minecraft:smooth_stone".formatted(FLOOR - 1, FLOOR - 1));

			for (int row = 0; row < ROWS.size(); row++) {
				String block = ROWS.get(row);
				int z = row * 3;

				for (int i = 0; i < COLORS.size(); i++) {
					int x = i * 2;
					String id = "dyeable_poplar:" + COLORS.get(i) + "_dyed_" + block;

					if (block.equals("poplar_door")) {
						server.runCommand(setblock(x, FLOOR, z, id + "[half=lower,facing=north]"));
						server.runCommand(setblock(x, FLOOR + 1, z, id + "[half=upper,facing=north]"));
					} else {
						server.runCommand(setblock(x, FLOOR, z, id));
					}
				}
			}

			// campfires, then a row with hay bales underneath for the taller signal smoke
			for (int i = 0; i < COLORS.size(); i++) {
				String campfire = "dyeable_poplar:" + COLORS.get(i) + "_dyed_campfire";
				server.runCommand(setblock(i * 2, FLOOR, 48, campfire));
				server.runCommand(setblock(i * 2, FLOOR, 51, "minecraft:hay_block"));
				server.runCommand(setblock(i * 2, FLOOR + 1, 51, campfire + "[signal_fire=true]"));
			}

			// boats and boats with chests sitting on the floor
			for (int i = 0; i < COLORS.size(); i++) {
				String prefix = "dyeable_poplar:" + COLORS.get(i) + "_dyed_poplar_";
				server.runCommand("summon %sboat %d %d 60 {Rotation:[90f,0f]}".formatted(prefix, i * 2, FLOOR));
				server.runCommand("summon %schest_boat %d %d 64 {Rotation:[90f,0f]}".formatted(prefix, i * 2, FLOOR));
			}

			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(20);

			shot(context, server, "1_logs_and_wood", 0);
			shot(context, server, "2_planks_stairs_slabs_fences", 12);
			shot(context, server, "3_gates_doors_trapdoors_plates", 24);
			shot(context, server, "4_buttons_signs_shelves", 36);

			camera(server, 15, 9, 40, 25);
			context.waitTicks(120); // let the smoke columns rise
			context.takeScreenshot("dyeable_poplar_5_campfires");

			camera(server, 15, 8, 52, 35);
			context.waitTicks(10);
			context.takeScreenshot("dyeable_poplar_6_boats");

			// close-ups of a few colours, where texture detail is easier to judge
			camera(server, 11, 4, 1, 40);
			context.waitTicks(10);
			context.takeScreenshot("dyeable_poplar_7_closeup_logs");

			camera(server, 11, 4, 25, 40);
			context.waitTicks(10);
			context.takeScreenshot("dyeable_poplar_8_closeup_doors");

			camera(server, 11, 4, 37, 40);
			context.waitTicks(10);
			context.takeScreenshot("dyeable_poplar_9_closeup_signs");

			camera(server, 11, 3, 56, 40);
			context.waitTicks(10);
			context.takeScreenshot("dyeable_poplar_10_closeup_boats");

			// item icons: a hotbar of one colour's items
			server.runCommand("gamemode creative @a");
			List<String> hotbar = List.of("poplar_door", "poplar_trapdoor", "poplar_sign", "poplar_hanging_sign",
					"poplar_shelf", "poplar_boat", "poplar_chest_boat", "campfire", "poplar_button");

			for (int slot = 0; slot < hotbar.size(); slot++) {
				server.runCommand("item replace entity @a hotbar.%d with dyeable_poplar:red_dyed_%s".formatted(slot, hotbar.get(slot)));
			}

			context.waitTicks(10);
			context.takeScreenshot("dyeable_poplar_11_hotbar_red");

			for (int slot = 0; slot < hotbar.size(); slot++) {
				server.runCommand("item replace entity @a hotbar.%d with dyeable_poplar:light_blue_dyed_%s".formatted(slot, hotbar.get(slot)));
			}

			context.waitTicks(10);
			context.takeScreenshot("dyeable_poplar_12_hotbar_light_blue");
		}
	}

	private static void shot(ClientGameTestContext context, TestServerContext server, String name, int rowZ) {
		camera(server, 15, 10, rowZ - 14, 35);
		context.waitTicks(10);
		context.takeScreenshot("dyeable_poplar_" + name);
	}

	/** Puts the (spectator) player above the floor at x/z, looking south (toward +z) and down by pitch degrees. */
	private static void camera(TestServerContext server, int x, int height, int z, int pitch) {
		server.runCommand("tp @a %d %d %d 0 %d".formatted(x, FLOOR + height, z, pitch));
	}

	private static String setblock(int x, int y, int z, String block) {
		return "setblock %d %d %d %s".formatted(x, y, z, block);
	}
}
