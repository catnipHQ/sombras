package com.livrodassombras.entity;

import com.livrodassombras.summon.Allegiance;
import com.livrodassombras.summon.SpiritBound;
import java.util.EnumSet;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Goals shared by the Livro dos Espíritos' creatures. They behave like the Ser de Sangue
 * (follow the summoner, attack whatever the summoner attacks or whatever hurts the summoner),
 * and they also guard: hostile monsters that come near the summoner are attacked first.
 * Works for any mob, tamed (Aliados) or not (Vex, Golem).
 */
public final class SpiritGoals {
    /** How far from the summoner a monster must be before the spirits go after it on their own. */
    public static final double GUARD_RADIUS = 14.0D;
    /** Ground summons farther than this from the summoner (and not fighting) appear next to them. */
    public static final double TELEPORT_DISTANCE = 24.0D;

    private SpiritGoals() {}

    /** The player this creature belongs to, if they are in the same level. */
    @Nullable
    public static Player summoner(Mob mob) {
        if (mob instanceof TamableAnimal pet) {
            return pet.getOwner() instanceof Player player ? player : null;
        }
        if (mob instanceof SpiritBound spirit) {
            UUID id = spirit.getSummonerUUID();
            return id == null ? null : mob.level().getPlayerByUUID(id);
        }
        return null;
    }

    /** Never the summoner's side; other players only if PvP rules allow it. */
    static boolean fairTarget(Player owner, LivingEntity target) {
        if (Allegiance.sameSide(target, owner)) {
            return false;
        }
        return !(target instanceof Player other) || owner.canHarmPlayer(other);
    }

    /** Monsters near the summoner, not creepers (they'd blow up next to the summoner) and not neutral mobs. */
    static boolean threatens(Mob mob, LivingEntity target) {
        Player owner = summoner(mob);
        return owner != null && target instanceof Enemy && !(target instanceof Creeper) && !(target instanceof NeutralMob)
                && target.distanceToSqr(owner) < GUARD_RADIUS * GUARD_RADIUS && fairTarget(owner, target);
    }

    /** Proactive guard: the nearest monster close to the summoner. */
    public static NearestAttackableTargetGoal<Mob> guard(Mob mob) {
        return new NearestAttackableTargetGoal<>(mob, Mob.class, 10, true, false, target -> threatens(mob, target));
    }

    /** Whatever hurt the summoner (OwnerHurtByTargetGoal for any mob). */
    public static class DefendSummonerGoal extends TargetGoal {
        @Nullable
        private LivingEntity attacker;
        private int timestamp;

        public DefendSummonerGoal(Mob mob) {
            super(mob, false);
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player owner = summoner(this.mob);
            if (owner == null) {
                return false;
            }
            this.attacker = owner.getLastHurtByMob();
            return owner.getLastHurtByMobTimestamp() != this.timestamp && this.attacker != null
                    && fairTarget(owner, this.attacker) && this.canAttack(this.attacker, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            this.mob.setTarget(this.attacker);
            Player owner = summoner(this.mob);
            if (owner != null) {
                this.timestamp = owner.getLastHurtByMobTimestamp();
            }
            super.start();
        }
    }

    /** Whatever the summoner attacked (OwnerHurtTargetGoal for any mob). */
    public static class AssistSummonerGoal extends TargetGoal {
        @Nullable
        private LivingEntity victim;
        private int timestamp;

        public AssistSummonerGoal(Mob mob) {
            super(mob, false);
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player owner = summoner(this.mob);
            if (owner == null) {
                return false;
            }
            this.victim = owner.getLastHurtMob();
            return owner.getLastHurtMobTimestamp() != this.timestamp && this.victim != null
                    && fairTarget(owner, this.victim) && this.canAttack(this.victim, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            this.mob.setTarget(this.victim);
            Player owner = summoner(this.mob);
            if (owner != null) {
                this.timestamp = owner.getLastHurtMobTimestamp();
            }
            super.start();
        }
    }

    /** Walks after the summoner (FollowOwnerGoal for any mob); far behind, it reappears next to them. */
    public static class FollowSummonerGoal extends Goal {
        private final PathfinderMob mob;
        private final double speed;
        private final float startDistance;
        private final float stopDistance;
        @Nullable
        private Player owner;
        private int recalc;

        public FollowSummonerGoal(PathfinderMob mob, double speed, float startDistance, float stopDistance) {
            this.mob = mob;
            this.speed = speed;
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player player = summoner(this.mob);
            if (player == null || player.isSpectator() || this.mob.getTarget() != null
                    || this.mob.distanceToSqr(player) < this.startDistance * this.startDistance) {
                return false;
            }
            this.owner = player;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.owner != null && this.owner.isAlive() && this.mob.getTarget() == null
                    && !this.mob.getNavigation().isDone()
                    && this.mob.distanceToSqr(this.owner) > this.stopDistance * this.stopDistance;
        }

        @Override
        public void start() {
            this.recalc = 0;
        }

        @Override
        public void stop() {
            this.owner = null;
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.owner == null) {
                return;
            }
            this.mob.getLookControl().setLookAt(this.owner, 10.0F, this.mob.getMaxHeadXRot());
            if (--this.recalc > 0) {
                return;
            }
            this.recalc = this.adjustedTickDelay(10);
            if (this.mob.distanceToSqr(this.owner) >= TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
                teleportNear(this.mob, this.owner);
            } else {
                this.mob.getNavigation().moveTo(this.owner, this.speed);
            }
        }
    }

    /** Puts {@code mob} on solid ground 2 to 3 blocks from {@code owner}, if there's room. */
    public static boolean teleportNear(Mob mob, Player owner) {
        Level level = mob.level();
        BlockPos centre = owner.blockPosition();
        for (int i = 0; i < 12; i++) {
            int dx = mob.getRandom().nextInt(7) - 3;
            int dz = mob.getRandom().nextInt(7) - 3;
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }
            BlockPos pos = centre.offset(dx, mob.getRandom().nextInt(3) - 1, dz);
            BlockPos below = pos.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                continue;
            }
            AABB box = mob.getBoundingBox().move(pos.getX() + 0.5D - mob.getX(), pos.getY() - mob.getY(),
                    pos.getZ() + 0.5D - mob.getZ());
            if (!level.noCollision(mob, box)) {
                continue;
            }
            mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, mob.getYRot(), mob.getXRot());
            mob.getNavigation().stop();
            return true;
        }
        return false;
    }
}
