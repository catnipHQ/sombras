package com.livrodassombras.ritual;

import com.livrodassombras.entity.SoulPortalEntity;
import com.livrodassombras.registry.ModSounds;
import com.livrodassombras.summon.Allegiance;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/** The Livro dos Espíritos' minor rituals. Server side only; costs and cooldowns are handled by {@link MinorRituals#cast}. */
public final class SpiritRituals {
    // Portal
    public static final double PORTAL_RANGE = 32.0D;
    private static final double PORTAL_NEAR = 2.5D;       // the first portal, in front of the caster
    private static final double PORTAL_MIN_GAP = 4.0D;

    // Fogo das Almas
    public static final double FIRE_RANGE = 7.0D;
    private static final double FIRE_COS = Math.cos(Math.toRadians(30.0D)); // a 60° cone
    public static final float FIRE_DAMAGE = 6.0F;         // 3 hearts, ignores armor
    public static final int FIRE_SECONDS = 3;

    // Troca
    public static final double SWAP_RANGE = 24.0D;

    private SpiritRituals() {}

    // ---------------------------------------------------------------- Portal

    /**
     * Two linked portals: one 2.5 blocks in front of the caster, the other where the caster is looking
     * (on the first surface, up to 32 blocks away). Both face the way the caster faces, so walking forward
     * through the near one comes out of the far one still walking forward. Open for 12 s; casting again
     * closes the old pair.
     */
    static boolean castPortal(ServerPlayer player, ServerLevel level) {
        Vec3 flat = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 near = standingSpot(level, player.position().add(flat.scale(PORTAL_NEAR)));

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(PORTAL_RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 aim;
        if (hit.getType() == HitResult.Type.MISS) {
            aim = eye.add(look.scale(PORTAL_RANGE));
        } else if (hit.getDirection() == Direction.UP) {
            aim = hit.getLocation();
        } else {
            // A wall or a ceiling: step back out of it, then down to the floor.
            aim = hit.getLocation().subtract(look.scale(0.8D)).subtract(0.0D, 0.9D, 0.0D);
        }
        Vec3 far = standingSpot(level, dropToFloor(level, player, aim));

        if (near == null || far == null) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.portal.sem_espaco"), true);
            return false;
        }
        if (near.distanceTo(far) < PORTAL_MIN_GAP) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.portal.perto"), true);
            return false;
        }
        if (!MinorRituals.payPrice(player, MinorRitual.PORTAL)) {
            return false;
        }

        SoulPortalEntity.closeAllOf(level, player);
        for (SoulPortalEntity portal : SoulPortalEntity.createPair(level, player, near, far, player.getYRot())) {
            level.addFreshEntity(portal);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, portal.getX(), portal.getY() + 1.0D, portal.getZ(),
                    30, 0.3D, 0.6D, 0.3D, 0.04D);
            level.playSound(null, portal.blockPosition(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 0.8F, 1.3F);
        }
        level.playSound(null, player.blockPosition(), ModSounds.SOUL_WHISPER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** Lets a point fall to the floor below it (up to 6 blocks); in mid-air it just floats there. */
    private static Vec3 dropToFloor(ServerLevel level, Player player, Vec3 point) {
        BlockHitResult floor = level.clip(new ClipContext(point, point.subtract(0.0D, 6.0D, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return floor.getType() == HitResult.Type.MISS ? point : floor.getLocation();
    }

    /** Somewhere a player fits standing up at {@code feet}, or a block above or below; null if none. */
    @Nullable
    private static Vec3 standingSpot(ServerLevel level, Vec3 feet) {
        for (double dy : new double[] {0.0D, 1.0D, -1.0D, 2.0D}) {
            Vec3 p = feet.add(0.0D, dy, 0.0D);
            if (level.noCollision(new AABB(p.x - 0.35D, p.y + 0.01D, p.z - 0.35D, p.x + 0.35D, p.y + 1.9D, p.z + 0.35D))) {
                return p;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- Fogo das Almas

    /** A cone of blue flames: everything within 7 blocks and 30° of where the caster looks, not behind a wall. */
    static boolean castSoulFire(ServerPlayer player, ServerLevel level) {
        if (!MinorRituals.payPrice(player, MinorRitual.FOGO_DAS_ALMAS)) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();

        AABB box = player.getBoundingBox().expandTowards(look.scale(FIRE_RANGE)).inflate(2.5D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !e.isSpectator() && !Allegiance.belongsTo(e, player))) {
            Vec3 centre = target.getBoundingBox().getCenter();
            Vec3 to = centre.subtract(eye);
            double distance = to.length();
            if (distance > FIRE_RANGE + target.getBbWidth() / 2.0D || distance < 1.0E-3D
                    || to.scale(1.0D / distance).dot(look) < FIRE_COS) {
                continue;
            }
            BlockHitResult wall = level.clip(new ClipContext(eye, centre, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (wall.getType() != HitResult.Type.MISS) {
                continue;
            }
            if (target instanceof Player other && !player.canHarmPlayer(other)) {
                continue;
            }
            DamageSource source = level.damageSources().indirectMagic(player, player);
            if (MinorRituals.hurtIgnoringCooldown(target, source, FIRE_DAMAGE)) {
                target.setSecondsOnFire(FIRE_SECONDS);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, centre.x, centre.y, centre.z, 12,
                        target.getBbWidth() * 0.4D, target.getBbHeight() * 0.3D, target.getBbWidth() * 0.4D, 0.02D);
            }
        }

        // The blast: flames thrown forward in a widening cone (count 0 = the offsets are the speed).
        Vec3 up = Math.abs(look.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = look.cross(up).normalize();
        Vec3 lift = side.cross(look).normalize();
        Vec3 mouth = eye.add(look.scale(0.6D)).subtract(0.0D, 0.2D, 0.0D);
        for (int i = 0; i < 70; i++) {
            double spread = Math.tan(Math.toRadians(28.0D)) * level.random.nextDouble();
            double a = level.random.nextDouble() * Math.PI * 2.0D;
            Vec3 dir = look.add(side.scale(Math.cos(a) * spread)).add(lift.scale(Math.sin(a) * spread)).normalize();
            double speed = 0.35D + level.random.nextDouble() * 0.35D;
            level.sendParticles(i % 5 == 0 ? ParticleTypes.SOUL : ParticleTypes.SOUL_FIRE_FLAME,
                    mouth.x, mouth.y, mouth.z, 0, dir.x, dir.y, dir.z, speed);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2F, 0.8F);
        return true;
    }

    // ---------------------------------------------------------------- Troca

    /** Swaps places with the creature or player in sight, up to 24 blocks away. Not on bosses. */
    static boolean castSwap(ServerPlayer player, ServerLevel level) {
        LivingEntity target = MinorRituals.findTarget(player, SWAP_RANGE);
        if (target == null) {
            MinorRituals.noTarget(player, SWAP_RANGE);
            return false;
        }
        if (target.getType().is(Tags.EntityTypes.BOSSES)) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.troca.chefe", target.getDisplayName()), true);
            return false;
        }

        Vec3 mine = player.position();
        Vec3 theirs = target.position();
        // Both must fit where the other one was.
        if (!level.noCollision(target, target.getBoundingBox().move(mine.subtract(theirs)))
                || !level.noCollision(player, player.getBoundingBox().move(theirs.subtract(mine)))) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.troca.sem_espaco"), true);
            return false;
        }
        if (!MinorRituals.payPrice(player, MinorRitual.TROCA)) {
            return false;
        }

        player.stopRiding();
        target.stopRiding();
        player.ejectPassengers();
        target.ejectPassengers();

        // A thread of souls between the two spots.
        Vec3 from = mine.add(0.0D, 1.0D, 0.0D);
        Vec3 to = theirs.add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
        int steps = Math.max(8, (int) (from.distanceTo(to) * 3.0D));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }

        player.connection.teleport(theirs.x, theirs.y, theirs.z, player.getYRot(), player.getXRot());
        if (target instanceof ServerPlayer other) {
            other.connection.teleport(mine.x, mine.y, mine.z, other.getYRot(), other.getXRot());
            other.displayClientMessage(Component.translatable("message.livro_das_sombras.troca.recebida", player.getDisplayName()), true);
        } else {
            target.teleportTo(mine.x, mine.y, mine.z);
            if (target instanceof Mob mob) {
                mob.getNavigation().stop();
            }
        }
        player.resetFallDistance();
        target.resetFallDistance();

        for (Vec3 spot : new Vec3[] {mine, theirs}) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spot.x, spot.y + 1.0D, spot.z, 25, 0.3D, 0.6D, 0.3D, 0.04D);
            level.sendParticles(ParticleTypes.SOUL, spot.x, spot.y + 1.0D, spot.z, 8, 0.3D, 0.6D, 0.3D, 0.02D);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.6F);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return true;
    }
}
