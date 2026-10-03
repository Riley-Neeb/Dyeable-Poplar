package com.fatul.dyeablepoplar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import net.fabricmc.fabric.api.event.player.BlockEvents;

/**
 * Right-clicking a placed poplar block (dyed or not) with a dye recolours it in place,
 * keeping its log axis, facing, half, slab type, fence connections, open state, waterlogging
 * and so on. Doors change both halves. One dye per click, none used in creative.
 */
public final class PoplarDyeing {
	private PoplarDyeing() {
	}

	static void register() {
		BlockEvents.USE_ITEM_ON.register(PoplarDyeing::useDyeOnBlock);
	}

	/** Returns null to let vanilla handle the click when it isn't a dye on dyeable poplar. */
	private static InteractionResult useDyeOnBlock(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hitResult) {
		DyeColor dye = stack.get(DataComponents.DYE);

		if (dye == null) {
			return null;
		}

		Block target = DyedPoplarBlocks.recolored(state.getBlock(), dye);

		if (target == null || !player.getAbilities().mayBuild) {
			return null;
		}

		// Don't swap a campfire that has food on it; the food would fall off.
		if (level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire
				&& campfire.getItems().stream().anyMatch(item -> !item.isEmpty())) {
			return null;
		}

		if (!level.isClientSide()) {
			if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
				DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
				BlockPos otherPos = pos.relative(half.getDirectionToOther());
				BlockState other = level.getBlockState(otherPos);

				if (other.is(state.getBlock())) {
					// Swap the other half quietly first, so neither half sees a mismatched partner and breaks.
					level.setBlock(otherPos, copySharedProperties(other, target.defaultBlockState()),
							Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
			}

			BlockState dyed = copySharedProperties(state, target.defaultBlockState());
			level.setBlock(pos, dyed, Block.UPDATE_ALL);

			if (target instanceof ButtonBlock || target instanceof BasePressurePlateBlock) {
				// A pressed button or plate still needs to pop back up after the swap.
				level.scheduleTick(pos, target, 1);
			}

			level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, dyed));
			player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
			stack.consume(1, player);
		}

		return InteractionResult.SUCCESS;
	}

	static BlockState copySharedProperties(BlockState from, BlockState to) {
		for (Property<?> property : from.getBlock().getStateDefinition().getProperties()) {
			if (to.hasProperty(property)) {
				to = copyProperty(from, to, property);
			}
		}

		return to;
	}

	private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
		return to.setValue(property, from.getValue(property));
	}
}
