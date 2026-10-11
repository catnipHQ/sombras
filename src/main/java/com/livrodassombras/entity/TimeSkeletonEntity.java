package com.livrodassombras.entity;

import com.livrodassombras.summon.Allegiance;
import com.livrodassombras.registry.ModEffects;
import com.livrodassombras.registry.ModEntities;
import com.livrodassombras.ritual.TimeRituals;
import com.livrodassombras.summon.TimeSummoning;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Esqueletinho do Tempo: a baby skeleton with a bow, summoned three at a time by the Livro do Tempo.
 * <ul>
 *   <li>A bit stronger than a skeleton: 22 health (skeleton 20), 2 armor, faster (it's small),
 *       and its arrows hit half a point harder. Doesn't burn in daylight.</li>
 *   <li>Lives for 4 minutes; the summoner sees the remaining time as a status effect.</li>
 *   <li>Follows and defends its owner like the Ser de Sangue (shoots whatever the owner attacks or whatever hurts the owner),
 *       but has no guard mode or ritual menu.</li>
 *   <li>Its arrows pass through its owner and the owner's pets and creatures instead of hitting them.</li>
 *   <li>Bone, not flesh: it never bleeds.</li>
 * </ul>
 */
public class TimeSkeletonEntity extends TamableAnimal implements RangedAttackMob {
    public static final int LIFETIME_TICKS = 20 * 60 * 4; // 4 minutes
    public static final int SQUAD_SIZE = 3;
    private static final int OWNER_GRACE_TICKS = 20 * 5;
    private static final double EXTRA_ARROW_DAMAGE = 0.5D;

    private int lifeTicks = LIFETIME_TICKS;
    private int ownerMissingTicks;

    public TimeSkeletonEntity(EntityType<? extends TimeSkeletonEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** A new skeleton for {@code owner}, bow in hand, not yet added to the level. */
    @Nullable
    public static TimeSkeletonEntity create(ServerLevel level, Player owner) {
        TimeSkeletonEntity skeleton = ModEntities.ESQUELETO_DO_TEMPO.get().create(level);
        if (skeleton != null) {
            skeleton.setTame(true);
            skeleton.setOwnerUUID(owner.getUUID());
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
        return skeleton;
    }

    /** Always a baby: half size, big head. */
    @Override
    public boolean isBaby() {
        return true;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new BowAttackGoal(1.0D, 30, 15.0F));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1D, 7.0F, 2.5F, false));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    // ---------------------------------------------------------------- lifetime

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !this.isAlive()) {
            return;
        }
        if (--this.lifeTicks <= 0) {
            this.dissipate();
            return;
        }

        LivingEntity owner = this.getOwner();
        if (owner == null) {
            if (++this.ownerMissingTicks > OWNER_GRACE_TICKS) {
                this.dissipate();
            }
            return;
        }
        this.ownerMissingTicks = 0;
        if (!owner.isAlive()) {
            this.dissipate();
            return;
        }

        // The owner's timer effect follows the squad's remaining time.
        MobEffectInstance timer = owner.getEffect(ModEffects.ESQUELETOS_DO_TEMPO.get());
        if (timer == null || timer.getDuration() < this.lifeTicks - 20) {
            owner.forceAddEffect(new MobEffectInstance(ModEffects.ESQUELETOS_DO_TEMPO.get(), this.lifeTicks, 0, false, false, true), this);
        }
    }

    /** Ends the skeleton with the Time path's vanishing (time up, owner gone). */
    public void dissipate() {
        if (this.level() instanceof ServerLevel level) {
            TimeSummoning.vanish(level, this.getX(), this.getY() + 0.4D, this.getZ());
        }
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!this.level().isClientSide && reason.shouldDestroy() && this.getOwner() instanceof ServerPlayer owner
                && squadOf(owner).isEmpty()) {
            owner.removeEffect(ModEffects.ESQUELETOS_DO_TEMPO.get());
            owner.displayClientMessage(Component.translatable("message.livro_das_sombras.esqueletos_foram"), true);
        }
    }

    /** The player's living skeletons in the player's level. */
    public static List<? extends TimeSkeletonEntity> squadOf(ServerPlayer player) {
        UUID id = player.getUUID();
        return player.serverLevel().getEntities(ModEntities.ESQUELETO_DO_TEMPO.get(),
                skeleton -> skeleton.isAlive() && !skeleton.isRemoved() && id.equals(skeleton.getOwnerUUID()));
    }

    /** Safety net, every second: no skeletons left, no timer. */
    public static void check(ServerPlayer player) {
        if (player.hasEffect(ModEffects.ESQUELETOS_DO_TEMPO.get()) && squadOf(player).isEmpty()) {
            player.removeEffect(ModEffects.ESQUELETOS_DO_TEMPO.get());
        }
    }

    public static void dissipateAllOf(ServerPlayer player) {
        for (TimeSkeletonEntity skeleton : squadOf(player)) {
            skeleton.dissipate();
        }
    }

    // ---------------------------------------------------------------- bow

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        // Arrows ignore the owner's side instead of hitting it.
        Arrow arrow = new Arrow(this.level(), this) {
            @Override
            protected boolean canHitEntity(Entity entity) {
                return super.canHitEntity(entity)
                        && !(entity instanceof LivingEntity living && TimeSkeletonEntity.this.isFriendly(living));
            }
        };
        arrow.setEnchantmentEffectsFromEntity(this, power);
        arrow.setBaseDamage(arrow.getBaseDamage() + EXTRA_ARROW_DAMAGE);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontal * 0.2D, dz, 1.6F, (float) (14 - this.level().getDifficulty().getId() * 4));
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    private boolean holdingBow() {
        return this.isHolding(Items.BOW);
    }

    /** The skeleton's bow fight: keep distance, strafe, draw for a second, shoot (adapted from the vanilla skeleton). */
    private class BowAttackGoal extends Goal {
        private final double speed;
        private final int interval;
        private final float maxDistanceSqr;
        private int attackTime = -1;
        private int seeTime;
        private boolean strafingClockwise;
        private boolean strafingBackwards;
        private int strafingTime = -1;

        BowAttackGoal(double speed, int interval, float maxDistance) {
            this.speed = speed;
            this.interval = interval;
            this.maxDistanceSqr = maxDistance * maxDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private TimeSkeletonEntity mob() {
            return TimeSkeletonEntity.this;
        }

        @Override
        public boolean canUse() {
            return this.mob().getTarget() != null && this.mob().holdingBow();
        }

        @Override
        public boolean canContinueToUse() {
            return (this.canUse() || !this.mob().getNavigation().isDone()) && this.mob().holdingBow();
        }

        @Override
        public void start() {
            super.start();
            this.mob().setAggressive(true);
        }

        @Override
        public void stop() {
            super.stop();
            this.mob().setAggressive(false);
            this.seeTime = 0;
            this.attackTime = -1;
            this.mob().stopUsingItem();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            TimeSkeletonEntity mob = this.mob();
            LivingEntity target = mob.getTarget();
            if (target == null) {
                return;
            }
            double distanceSqr = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            boolean sees = mob.getSensing().hasLineOfSight(target);
            if (sees != this.seeTime > 0) {
                this.seeTime = 0;
            }
            this.seeTime += sees ? 1 : -1;

            if (distanceSqr <= this.maxDistanceSqr && this.seeTime >= 20) {
                mob.getNavigation().stop();
                this.strafingTime++;
            } else {
                mob.getNavigation().moveTo(target, this.speed);
                this.strafingTime = -1;
            }

            if (this.strafingTime >= 20) {
                if (mob.getRandom().nextFloat() < 0.3F) {
                    this.strafingClockwise = !this.strafingClockwise;
                }
                if (mob.getRandom().nextFloat() < 0.3F) {
                    this.strafingBackwards = !this.strafingBackwards;
                }
                this.strafingTime = 0;
            }

            if (this.strafingTime > -1) {
                if (distanceSqr > this.maxDistanceSqr * 0.75F) {
                    this.strafingBackwards = false;
                } else if (distanceSqr < this.maxDistanceSqr * 0.25F) {
                    this.strafingBackwards = true;
                }
                mob.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
                mob.lookAt(target, 30.0F, 30.0F);
            } else {
                mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            if (mob.isUsingItem()) {
                if (!sees && this.seeTime < -60) {
                    mob.stopUsingItem();
                } else if (sees) {
                    int drawn = mob.getTicksUsingItem();
                    if (drawn >= 20) {
                        mob.stopUsingItem();
                        mob.performRangedAttack(target, BowItem.getPowerForTime(drawn));
                        this.attackTime = this.interval;
                    }
                }
            } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
                mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(mob, Items.BOW));
            }
        }
    }

    // ---------------------------------------------------------------- loyalty

    /** The owner, the owner's pets and the owner's other creatures (the rest of the squad, the Ser de Sangue). */
    private boolean isFriendly(LivingEntity target) {
        if (target == this || this.isOwnedBy(target)) {
            return true;
        }
        LivingEntity owner = this.getOwner();
        return Allegiance.belongsTo(target, owner);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !this.isFriendly(target) && super.canAttack(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && this.isFriendly(target)) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (this.isFriendly(target)) {
            return false;
        }
        if (target instanceof Player targetPlayer && owner instanceof Player ownerPlayer
                && !ownerPlayer.canHarmPlayer(targetPlayer)) {
            return false; // respects PvP settings and teams
        }
        return true;
    }

    // ---------------------------------------------------------------- misc

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SKELETON_STEP, 0.15F, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", this.lifeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("LifeTicks")) {
            this.lifeTicks = tag.getInt("LifeTicks");
        }
    }
}
