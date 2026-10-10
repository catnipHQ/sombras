package com.livrodassombras.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A blood stain lying flat on the ground. Stays a few seconds, then fades away. */
public class BloodTrailParticle extends TextureSheetParticle {
    private final float angle;

    protected BloodTrailParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.lifetime = 70 + this.random.nextInt(30); // 3.5 to 5 seconds
        this.quadSize = 0.10F + this.random.nextFloat() * 0.08F;
        this.angle = this.random.nextFloat() * Mth.TWO_PI;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        float shade = 0.75F + this.random.nextFloat() * 0.25F;
        this.setColor(shade, shade, shade);
    }

    @Override
    public void tick() {
        super.tick();
        // Fade out over the last 40% of its life.
        float left = 1.0F - (float) this.age / this.lifetime;
        this.alpha = Mth.clamp(left / 0.4F, 0.0F, 1.0F) * 0.9F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Drawn as a horizontal square (both sides) instead of facing the camera. */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x());
        float y = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y());
        float z = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z());
        float size = this.getQuadSize(partialTick);
        float cos = Mth.cos(this.angle) * size;
        float sin = Mth.sin(this.angle) * size;

        float[][] corners = {{-cos + sin, -sin - cos}, {-cos - sin, -sin + cos}, {cos - sin, sin + cos}, {cos + sin, sin - cos}};
        float[][] uvs = {{this.getU1(), this.getV1()}, {this.getU1(), this.getV0()}, {this.getU0(), this.getV0()}, {this.getU0(), this.getV1()}};
        int light = this.getLightColor(partialTick);

        for (int i = 0; i < 4; i++) {
            vertex(buffer, x + corners[i][0], y, z + corners[i][1], uvs[i], light);
        }
        for (int i = 3; i >= 0; i--) {
            vertex(buffer, x + corners[i][0], y, z + corners[i][1], uvs[i], light);
        }
    }

    private void vertex(VertexConsumer buffer, float x, float y, float z, float[] uv, int light) {
        buffer.vertex(x, y, z).uv(uv[0], uv[1]).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new BloodTrailParticle(level, x, y, z, this.sprites);
        }
    }
}
