package com.fatul.dyeablepoplar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A campfire built from dyed logs. It works exactly like a vanilla campfire (cooking, signal
 * fires, putting it out), except that the smoke column is the colour of the dye.
 */
public class DyedCampfireBlock extends CampfireBlock {
	private final DyeColor color;

	public DyedCampfireBlock(DyeColor color, Properties properties) {
		super(true, 1, properties);
		this.color = color;
	}

	public DyeColor color() {
		return color;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return state.getValue(LIT) ? createTickerHelper(type, BlockEntityTypes.CAMPFIRE, this::smokeTick) : null;
		}

		return super.getTicker(level, state, type);
	}

	/** Vanilla's campfire particle tick, with the smoke column swapped for the dyed one. */
	private void smokeTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
		RandomSource random = level.getRandom();

		if (random.nextFloat() < 0.11F) {
			SimpleParticleType smoke = state.getValue(SIGNAL_FIRE)
					? DyedPoplarParticles.signalSmoke(color)
					: DyedPoplarParticles.cosySmoke(color);

			for (int i = 0; i < random.nextInt(2) + 2; i++) {
				level.addAlwaysVisibleParticle(smoke, true,
						pos.getX() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
						pos.getY() + random.nextDouble() + random.nextDouble(),
						pos.getZ() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
						0.0, 0.07, 0.0);
			}
		}

		// Food cooking on the fire still gives off ordinary little smoke puffs, as in vanilla.
		int facing = state.getValue(FACING).get2DDataValue();
		NonNullList<ItemStack> items = campfire.getItems();

		for (int slot = 0; slot < items.size(); slot++) {
			if (!items.get(slot).isEmpty() && random.nextFloat() < 0.2F) {
				Direction direction = Direction.from2DDataValue(Math.floorMod(slot + facing, 4));
				double x = pos.getX() + 0.5 - direction.getStepX() * 0.3125F + direction.getClockWise().getStepX() * 0.3125F;
				double y = pos.getY() + 0.5;
				double z = pos.getZ() + 0.5 - direction.getStepZ() * 0.3125F + direction.getClockWise().getStepZ() * 0.3125F;

				for (int puff = 0; puff < 4; puff++) {
					level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 5.0E-4, 0.0);
				}
			}
		}
	}
}
