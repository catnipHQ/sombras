package com.livrodassombras.entity;

import com.livrodassombras.registry.ModEntities;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One end of a Portal ritual: a round, player-sized disc of blue soul fire, standing upright.
 * The caster places the first one, then the second; once both are open they are linked and anything that
 * passes through one (players, creatures, items, arrows) comes out of the other, on the far side, still
 * moving the same way. They stay open until the caster removes one (Shift + right-click with the book,
 * looking at it) or leaves the game, dies or casts a new pair.
 */
public class SoulPortalEntity extends Entity {
    /** Each thing can pass at most once every half second (so it doesn't bounce back and forth). */
    private static final int PASS_COOLDOWN = 10;
    private static final String PASS_TAG = "veritates_portal_cd";
    private static final double RADIUS = 0.95D;
    private static final double CENTRE_HEIGHT = 1.0D;
    /** How far away a portal can be removed from. */
    private static final double REMOVE_REACH = 8.0D;

    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(SoulPortalEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    /** The other end; null while this is the first portal and the second hasn't been placed. */
    @Nullable
    private UUID partnerId;
    private int age;

    public SoulPortalEntity(EntityType<? extends SoulPortalEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Opens one (still unlinked) portal for {@code owner} at {@code at}, facing {@code yaw}. */
    @Nullable
    public static SoulPortalEntity place(ServerLevel level, ServerPlayer owner, Vec3 at, float yaw) {
        SoulPortalEntity portal = ModEntities.PORTAL_DE_ALMAS.get().create(level);
        if (portal == null) {
            return null;
        }
        portal.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        portal.entityData.set(OWNER, Optional.of(owner.getUUID()));
        level.addFreshEntity(portal);
        return portal;
    }

    /** Links two portals: from now on each one leads to the other. */
    public static void link(SoulPortalEntity a, SoulPortalEntity b) {
        a.partnerId = b.getUUID();
        b.partnerId = a.getUUID();
    }

    /** The player's first portal, still waiting for its pair; null if there isn't one. */
    @Nullable
    public static SoulPortalEntity pendingOf(ServerLevel level, ServerPlayer owner) {
        UUID id = owner.getUUID();
        List<? extends SoulPortalEntity> waiting = level.getEntities(ModEntities.PORTAL_DE_ALMAS.get(),
                p -> p.isAlive() && p.partnerId == null && p.isOwnedBy(id));
        return waiting.isEmpty() ? null : waiting.get(0);
    }

    /** True if the player already has any portal open (linked or not). */
    public static boolean hasAny(ServerLevel level, ServerPlayer owner) {
        UUID id = owner.getUUID();
        return !level.getEntities(ModEntities.PORTAL_DE_ALMAS.get(), p -> p.isAlive() && p.isOwnedBy(id)).isEmpty();
    }

    /** Closes every portal of the player, in every dimension. */
    public static void closeAllOf(ServerLevel level, ServerPlayer owner) {
        UUID id = owner.getUUID();
        for (ServerLevel other : level.getServer().getAllLevels()) {
            for (SoulPortalEntity portal : other.getEntities(ModEntities.PORTAL_DE_ALMAS.get(),
                    p -> p.isAlive() && p.isOwnedBy(id))) {
                portal.close();
            }
        }
    }

    public boolean isOwnedBy(UUID id) {
        return this.entityData.get(OWNER).map(id::equals).orElse(false);
    }

    /** The player's portal they are looking at (up to 8 blocks, not through walls), or null. Works on both sides. */
    @Nullable
    public static SoulPortalEntity lookedAt(Player player) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(REMOVE_REACH));
        BlockHitResult wall = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }

        UUID id = player.getUUID();
        SoulPortalEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (SoulPortalEntity portal : level.getEntitiesOfClass(SoulPortalEntity.class, new AABB(eye, end).inflate(2.0D),
                p -> p.isAlive() && p.isOwnedBy(id))) {
            AABB box = portal.getBoundingBox().inflate(0.2D);
            double distance;
            if (box.contains(eye)) {
                distance = 0.0D;
            } else {
                Optional<Vec3> hit = box.clip(eye, end);
                if (hit.isEmpty()) {
                    continue;
                }
                distance = hit.get().distanceToSqr(eye);
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = portal;
            }
        }
        return best;
    }

    /** 0 to 1: grows open over 0.4 s. */
    public float openness(float partialTick) {
        return Mth.clamp((this.tickCount + partialTick) / 8.0F, 0.0F, 1.0F);
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
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        // The portals go with the caster: logged off, they close.
        if (this.tickCount % 20 == 0) {
            Optional<UUID> owner = this.entityData.get(OWNER);
            if (owner.isEmpty() || level.getServer().getPlayerList().getPlayer(owner.get()) == null) {
                this.close();
                return;
            }
        }
        if (this.age < 6) {
            this.age++;
            return; // still opening
        }
        if (this.partnerId == null) {
            return; // the first portal, waiting for the second
        }
        if (!(level.getEntity(this.partnerId) instanceof SoulPortalEntity partner) || !partner.isAlive()) {
            this.close(); // the other end is gone (removed, or its caster left)
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

    /** Closes this portal and the one it leads to. */
    public void closeWithPartner() {
        if (this.partnerId != null && this.level() instanceof ServerLevel level
                && level.getEntity(this.partnerId) instanceof SoulPortalEntity partner) {
            partner.close();
        }
        this.close();
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
        this.entityData.define(OWNER, Optional.empty());
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
        this.entityData.set(OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        this.age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.partnerId != null) {
            tag.putUUID("Partner", this.partnerId);
        }
        this.entityData.get(OWNER).ifPresent(id -> tag.putUUID("Owner", id));
        tag.putInt("Age", this.age);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
