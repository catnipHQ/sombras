package com.livrodassombras.entity;

import com.livrodassombras.registry.ModEntities;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One end of a Portal ritual: a round, player-sized disc of blue soul fire, standing upright.
 * Two of them are linked; anything that passes through one (players, creatures, items, arrows)
 * comes out of the other, on the far side, still moving the same way. Both close after 12 seconds.
 */
public class SoulPortalEntity extends Entity {
    public static final int DURATION = 20 * 12;
    /** Each thing can pass at most once every half second (so it doesn't bounce back and forth). */
    private static final int PASS_COOLDOWN = 10;
    private static final String PASS_TAG = "veritates_portal_cd";
    private static final double RADIUS = 0.95D;
    private static final double CENTRE_HEIGHT = 1.0D;

    @Nullable
    private UUID partnerId;
    @Nullable
    private UUID ownerId;
    private int age;

    public SoulPortalEntity(EntityType<? extends SoulPortalEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Opens a linked pair, both facing {@code yaw}. Not yet added to the level. */
    public static SoulPortalEntity[] createPair(ServerLevel level, ServerPlayer owner, Vec3 a, Vec3 b, float yaw) {
        SoulPortalEntity first = ModEntities.PORTAL_DE_ALMAS.get().create(level);
        SoulPortalEntity second = ModEntities.PORTAL_DE_ALMAS.get().create(level);
        if (first == null || second == null) {
            return new SoulPortalEntity[0];
        }
        first.moveTo(a.x, a.y, a.z, yaw, 0.0F);
        second.moveTo(b.x, b.y, b.z, yaw, 0.0F);
        first.partnerId = second.getUUID();
        second.partnerId = first.getUUID();
        first.ownerId = owner.getUUID();
        second.ownerId = owner.getUUID();
        return new SoulPortalEntity[] {first, second};
    }

    /** Closes the player's previous pair (only one pair per player). */
    public static void closeAllOf(ServerLevel level, ServerPlayer owner) {
        UUID id = owner.getUUID();
        for (SoulPortalEntity portal : level.getEntities(ModEntities.PORTAL_DE_ALMAS.get(),
                p -> p.isAlive() && id.equals(p.ownerId))) {
            portal.close();
        }
    }

    /** 0 to 1: grows open over 0.4 s and shrinks closed over the last 0.4 s. */
    public float openness(float partialTick) {
        float t = this.tickCount + partialTick;
        return Mth.clamp(Math.min(t / 8.0F, (DURATION - t) / 8.0F), 0.0F, 1.0F);
    }

    /** The direction the disc faces (horizontal). */
    private Vec3 forward() {
        return Vec3.directionFromRotation(0.0F, this.getYRot());
    }

    private Vec3 centre() {
        return this.position().add(0.0D, CENTRE_HEIGHT, 0.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.clientEffects();
            return;
        }
        if (++this.age >= DURATION) {
            this.close();
            return;
        }
        if (this.age < 6 || !(this.level() instanceof ServerLevel level)) {
            return; // still opening
        }
        if (!(this.partnerId != null && level.getEntity(this.partnerId) instanceof SoulPortalEntity partner) || !partner.isAlive()) {
            this.close();
            return;
        }

        Vec3 c = this.centre();
        Vec3 f = this.forward();
        Vec3 right = new Vec3(-f.z, 0.0D, f.x);
        long now = level.getGameTime();
        for (Entity entity : level.getEntities(this, this.getBoundingBox().inflate(1.5D),
                e -> !(e instanceof SoulPortalEntity) && e.isAlive() && !e.isSpectator() && !e.isPassenger() && !e.isVehicle())) {
            if (entity.getPersistentData().getLong(PASS_TAG) > now) {
                continue;
            }
            // Where its middle was last tick and is now, relative to the disc.
            double h = entity.getBbHeight() / 2.0D;
            Vec3 now3 = entity.position().add(0.0D, h, 0.0D).subtract(c);
            Vec3 old3 = new Vec3(entity.xo, entity.yo + h, entity.zo).subtract(c);
            double depthNow = now3.dot(f);
            double depthOld = old3.dot(f);
            boolean crossed = (depthNow <= 0.0D) != (depthOld <= 0.0D) || Math.abs(depthNow) < 0.25D;
            if (!crossed) {
                continue;
            }
            // Inside the circle where it crosses the plane.
            double t = Math.abs(depthOld - depthNow) < 1.0E-4D ? 1.0D : Mth.clamp(depthOld / (depthOld - depthNow), 0.0D, 1.0D);
            Vec3 at = old3.lerp(now3, t);
            double lateral = at.dot(right);
            double vertical = at.y;
            if (lateral * lateral + vertical * vertical > RADIUS * RADIUS + 0.15D) {
                continue;
            }
            double side = depthOld > 0.0D ? 1.0D : depthOld < 0.0D ? -1.0D : -Math.signum(depthNow == 0.0D ? 1.0D : depthNow);
            this.send(entity, partner, side, lateral, vertical, now);
        }
    }

    /**
     * Moves {@code entity} out of {@code partner}: on the opposite side from where it came in,
     * at the same spot on the disc, turned and still moving the same way relative to the portals.
     */
    private void send(Entity entity, SoulPortalEntity partner, double side, double lateral, double vertical, long now) {
        float turn = partner.getYRot() - this.getYRot();
        Vec3 pf = partner.forward();
        Vec3 pr = new Vec3(-pf.z, 0.0D, pf.x);
        double out = 0.6D + entity.getBbWidth() / 2.0D;
        Vec3 exit = partner.centre()
                .add(pf.scale(-side * out))
                .add(pr.scale(Mth.clamp(lateral, -0.6D, 0.6D)))
                .add(0.0D, vertical - entity.getBbHeight() / 2.0D, 0.0D);
        Vec3 motion = entity.getDeltaMovement().yRot(-turn * Mth.DEG_TO_RAD);
        float yaw = entity.getYRot() + turn;

        entity.getPersistentData().putLong(PASS_TAG, now + PASS_COOLDOWN);
        ServerLevel level = (ServerLevel) this.level();
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + entity.getBbHeight() / 2.0D, entity.getZ(),
                12, 0.2D, 0.3D, 0.2D, 0.03D);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8F, 1.4F);

        if (entity instanceof ServerPlayer player) {
            player.connection.teleport(exit.x, exit.y, exit.z, yaw, player.getXRot());
        } else {
            entity.teleportTo(exit.x, exit.y, exit.z);
            entity.setYRot(yaw);
            entity.setYHeadRot(yaw);
            entity.setDeltaMovement(motion);
            entity.hasImpulse = true;
        }
        entity.resetFallDistance();

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, exit.x, exit.y + entity.getBbHeight() / 2.0D, exit.z,
                12, 0.2D, 0.3D, 0.2D, 0.03D);
        level.playSound(null, exit.x, exit.y, exit.z, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    /** Closes with a last flicker of blue fire. */
    public void close() {
        if (this.level() instanceof ServerLevel level && this.isAlive()) {
            Vec3 c = this.centre();
            level.sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z, 10, 0.3D, 0.5D, 0.3D, 0.02D);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.7F, 0.6F);
        }
        this.discard();
    }

    private void clientEffects() {
        float open = this.openness(0.0F);
        if (open <= 0.0F) {
            return;
        }
        // Blue flames running around the rim.
        Vec3 c = this.centre();
        Vec3 f = this.forward();
        Vec3 right = new Vec3(-f.z, 0.0D, f.x);
        for (int i = 0; i < 2; i++) {
            double a = this.tickCount * 0.35D + i * Math.PI + this.random.nextDouble() * 0.3D;
            double r = RADIUS * open;
            Vec3 p = c.add(right.scale(Math.cos(a) * r)).add(0.0D, Math.sin(a) * r, 0.0D);
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 0.0D, 0.01D, 0.0D);
        }
        if (this.random.nextInt(4) == 0) {
            Vec3 p = c.add(right.scale((this.random.nextDouble() - 0.5D) * RADIUS)).add(0.0D, (this.random.nextDouble() - 0.5D) * RADIUS, 0.0D);
            this.level().addParticle(ParticleTypes.SOUL, p.x, p.y, p.z, f.x * 0.02D, 0.02D, f.z * 0.02D);
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 96.0D * 96.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.partnerId = tag.hasUUID("Partner") ? tag.getUUID("Partner") : null;
        this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.partnerId != null) {
            tag.putUUID("Partner", this.partnerId);
        }
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        tag.putInt("Age", this.age);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
