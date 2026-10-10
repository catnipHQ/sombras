package com.livrodassombras.entity;

import com.livrodassombras.registry.ModEffects;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * Ser de Sangue: a zombie-like servant summoned with the Livro do Sangue.
 * <ul>
 *   <li>Stronger than a zombie: 26 health (zombie 20), 5 attack (zombie 3), doesn't burn in daylight.</li>
 *   <li>Lives for 2 minutes; the summoner sees the remaining time as a status effect.</li>
 *   <li>Follows and defends its owner like a wolf: attacks whatever the owner attacks or whatever hurts the owner.</li>
 *   <li>Right-click toggles guard mode: it stays at its post, still defends, then returns to the post.</li>
 *   <li>Never attacks its owner, its owner's pets or its owner's other creatures.</li>
 * </ul>
 */
public class BloodBeingEntity extends TamableAnimal {
    public static final int LIFETIME_TICKS = 20 * 120; // 2 minutes
    /** How long the creature waits for an owner who logged off or changed dimension before dissipating. */
    private static final int OWNER_GRACE_TICKS = 20 * 5;

    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.0F), 1.3F);

    private int lifeTicks = LIFETIME_TICKS;
    private int ownerMissingTicks;
    private boolean guarding;
    @Nullable
    private BlockPos guardPos;

    public BloodBeingEntity(EntityType<? extends BloodBeingEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 35.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new ReturnToGuardPostGoal());
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1D, 6.0F, 2.0F, false) {
            @Override
            public boolean canUse() {
                return !BloodBeingEntity.this.guarding && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !BloodBeingEntity.this.guarding && super.canContinueToUse();
            }
        });
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

        // Keep the owner's timer effect in step with the real remaining time.
        MobEffectInstance timer = owner.getEffect(ModEffects.SER_DE_SANGUE.get());
        if (timer == null || Math.abs(timer.getDuration() - this.lifeTicks) > 20) {
            owner.forceAddEffect(new MobEffectInstance(ModEffects.SER_DE_SANGUE.get(), this.lifeTicks, 0, false, false, true), this);
        }

        if (this.tickCount % 10 == 0 && this.level() instanceof ServerLevel level) {
            level.sendParticles(BLOOD, this.getX(), this.getY() + 0.1D, this.getZ(), 1, 0.2D, 0.0D, 0.2D, 0.0D);
        }
    }

    /** Ends the creature in a burst of blood (time up, owner gone). */
    public void dissipate() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(BLOOD, this.getX(), this.getY() + 1.0D, this.getZ(), 50, 0.35D, 0.8D, 0.35D, 0.03D);
            level.playSound(null, this.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1.0F, 0.5F);
            if (this.getOwner() instanceof Player owner) {
                owner.displayClientMessage(Component.translatable("message.livro_das_sombras.ser_dissipado"), true);
            }
        }
        this.discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.clearOwnerTimer();
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(BLOOD, this.getX(), this.getY() + 1.0D, this.getZ(), 40, 0.35D, 0.8D, 0.35D, 0.03D);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            this.clearOwnerTimer();
        }
        super.remove(reason);
    }

    private void clearOwnerTimer() {
        LivingEntity owner = this.getOwner();
        if (owner != null) {
            owner.removeEffect(ModEffects.SER_DE_SANGUE.get());
        }
    }

    /** True if {@code player} already has a living Ser de Sangue in this level. */
    public static boolean hasBeing(ServerLevel level, Player player) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof BloodBeingEntity being && being.isAlive() && being.isOwnedBy(player)) {
                return true;
            }
        }
        return false;
    }

    /** Dissipates every Ser de Sangue owned by {@code player} in this level. */
    public static void dissipateAllOf(ServerLevel level, Player player) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof BloodBeingEntity being && being.isAlive() && being.isOwnedBy(player)) {
                being.dissipate();
            }
        }
    }

    // ---------------------------------------------------------------- guard mode

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !this.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide) {
            this.guarding = !this.guarding;
            if (this.guarding) {
                this.guardPos = this.blockPosition();
                this.getNavigation().stop();
                player.sendSystemMessage(Component.translatable("message.livro_das_sombras.modo_guarda"));
            } else {
                this.guardPos = null;
                player.sendSystemMessage(Component.translatable("message.livro_das_sombras.modo_seguir"));
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /** In guard mode, walks back to the post once there is nothing left to fight. */
    private class ReturnToGuardPostGoal extends Goal {
        ReturnToGuardPostGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        private boolean awayFromPost() {
            BlockPos post = BloodBeingEntity.this.guardPos;
            return BloodBeingEntity.this.guarding && post != null && BloodBeingEntity.this.getTarget() == null
                    && BloodBeingEntity.this.distanceToSqr(post.getX() + 0.5D, post.getY(), post.getZ() + 0.5D) > 2.25D;
        }

        @Override
        public boolean canUse() {
            return this.awayFromPost();
        }

        @Override
        public boolean canContinueToUse() {
            return this.awayFromPost() && !BloodBeingEntity.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            BlockPos post = BloodBeingEntity.this.guardPos;
            if (post != null) {
                BloodBeingEntity.this.getNavigation().moveTo(post.getX() + 0.5D, post.getY(), post.getZ() + 0.5D, 1.0D);
            }
        }
    }

    // ---------------------------------------------------------------- loyalty

    /** Never the owner, nor anything that belongs to the owner. */
    private boolean isFriendly(LivingEntity target) {
        if (target == this || this.isOwnedBy(target)) {
            return true;
        }
        LivingEntity owner = this.getOwner();
        if (owner == null) {
            return false;
        }
        return target instanceof TamableAnimal pet && pet.isOwnedBy(owner);
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
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putBoolean("Guarding", this.guarding);
        if (this.guardPos != null) {
            tag.put("GuardPos", NbtUtils.writeBlockPos(this.guardPos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("LifeTicks")) {
            this.lifeTicks = tag.getInt("LifeTicks");
        }
        this.guarding = tag.getBoolean("Guarding");
        this.guardPos = tag.contains("GuardPos") ? NbtUtils.readBlockPos(tag.getCompound("GuardPos")) : null;
    }
}
