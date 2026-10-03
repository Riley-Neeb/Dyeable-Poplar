package com.fatul.dyeablepoplar.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * Campfire smoke tinted with a dye colour. Moves, lives and fades exactly like vanilla campfire
 * smoke; only the colour differs.
 */
final class DyedCampfireSmokeParticle extends SingleQuadParticle {
	DyedCampfireSmokeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
			boolean signal, TextureAtlasSprite sprite, float red, float green, float blue) {
		super(level, x, y, z, sprite);
		this.scale(3.0F);
		this.setSize(0.25F, 0.25F);
		this.lifetime = signal ? this.random.nextInt(50) + 280 : this.random.nextInt(50) + 80;
		this.gravity = 3.0E-6F;
		this.xd = xd;
		this.yd = yd + this.random.nextFloat() / 500.0F;
		this.zd = zd;
		this.setColor(red, green, blue);
		this.setAlpha(signal ? 0.95F : 0.9F);
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		if (this.age++ < this.lifetime && this.alpha > 0.0F) {
			this.xd += this.random.nextFloat() / 5000.0F * (this.random.nextBoolean() ? 1 : -1);
			this.zd += this.random.nextFloat() / 5000.0F * (this.random.nextBoolean() ? 1 : -1);
			this.yd -= this.gravity;
			this.move(this.xd, this.yd, this.zd);

			if (this.age >= this.lifetime - 60 && this.alpha > 0.01F) {
				this.alpha -= 0.015F;
			}
		} else {
			this.remove();
		}
	}

	@Override
	public SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	/** Makes the smoke for one dye colour, normal or signal-fire. */
	static final class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;
		private final boolean signal;
		private final float red;
		private final float green;
		private final float blue;

		Provider(SpriteSet sprites, boolean signal, int rgb) {
			this.sprites = sprites;
			this.signal = signal;
			this.red = ((rgb >> 16) & 0xFF) / 255.0F;
			this.green = ((rgb >> 8) & 0xFF) / 255.0F;
			this.blue = (rgb & 0xFF) / 255.0F;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
				double xd, double yd, double zd, RandomSource random) {
			return new DyedCampfireSmokeParticle(level, x, y, z, xd, yd, zd, signal, sprites.get(random), red, green, blue);
		}
	}
}
