package com.livrodassombras.entity;

import com.livrodassombras.registry.ModEffects;
import com.livrodassombras.registry.ModEntities;
import com.livrodassombras.summon.Allegiance;
import com.livrodassombras.summon.SpiritSummoning;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Aliado Espiritual: a hooded ghost summoned three at a time by the Livro dos Espíritos.
 * <ul>
 *   <li>Support, not damage: 16 health, 2 attack, 2 armor, but every hit leaves the target with
 *       Weakness I (5 s) and Slowness I (4 s).</li>
 *   <li>Lives for 60 s; the summoner sees the remaining time as a status effect.</li>
 *   <li>Follows and defends its summoner like the Ser de Sangue, and also goes after monsters that
 *       come close to the summoner. No guard mode or ritual menu.</li>
 *   <li>A spirit: doesn't burn, doesn't take fall damage, and its summoner's side can't hurt it.</li>
 * </ul>
 */
public class SoulAllyEntity extends TamableAnimal {
    public static final int LIFETIME_TICKS = 20 * 60;
    public static final int SQUAD_SIZE = 3;
    private static final int OWNER_GRACE_TICKS = 20 * 5;
    public static final int WEAKNESS_TICKS = 20 * 5;
    public static final int SLOWNESS_TICKS = 20 * 4;

    private int lifeTicks = LIFETIME_TICKS;
    private int ownerMissingTicks;

    public SoulAllyEntity(EntityType<? extends SoulAllyEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** A new ally for {@code owner}, not yet added to the level. */
    @Nullable
    public static SoulAllyEntity create(ServerLevel level, Player owner) {
        SoulAllyEntity ally = ModEntities.ALIADO_ESPIRITUAL.get().create(level);
        if (ally != null) {
            ally.setTame(true);
            ally.setOwnerUUID(owner.getUUID());
        }
        return ally;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1D, 6.0F, 2.0F, false));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, SpiritGoals.guard(this));
    }

    /** Every hit weakens and slows: the Vex and the Golem do the damage, the allies hold the enemy down. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0), this);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_TICKS, 0), this);
            if (this.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SOUL, living.getX(), living.getY() + living.getBbHeight() * 0.6D,
                        living.getZ(), 6, living.getBbWidth() * 0.3D, living.getBbHeight() * 0.25D,
                        living.getBbWidth() * 0.3D, 0.02D);
            }
        }
        return hit;
    }

    // ---------------------------------------------------------------- lifetime

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            // Its legs fade into blue flame.
            if (this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.4D), this.getY() + this.random.nextDouble() * 0.5D,
                        this.getRandomZ(0.4D), 0.0D, 0.015D, 0.0D);
            }
            return;
        }
        if (!this.isAlive()) {
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

        MobEffectInstance timer = owner.getEffect(ModEffects.ALIADOS_ESPIRITUAIS.get());
        if (timer == null || timer.getDuration() < this.lifeTicks - 20) {
            owner.forceAddEffect(new MobEffectInstance(ModEffects.ALIADOS_ESPIRITUAIS.get(), this.lifeTicks, 0, false, false, true), this);
        }
    }

    /** Ends the ally in blue soul fire (time up, summoner gone). */
    public void dissipate() {
        if (this.level() instanceof ServerLevel level) {
            SpiritSummoning.vanish(level, this.getX(), this.getY() + 1.0D, this.getZ());
        }
        this.discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            SpiritSummoning.vanish(level, this.getX(), this.getY() + 1.0D, this.getZ());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!this.level().isClientSide && reason.shouldDestroy() && this.getOwner() instanceof ServerPlayer owner
                && squadOf(owner).isEmpty()) {
            owner.removeEffect(ModEffects.ALIADOS_ESPIRITUAIS.get());
            owner.displayClientMessage(Component.translatable("message.livro_das_sombras.aliados_foram"), true);
        }
    }

    /** The player's living allies in the player's level. */
    public static List<? extends SoulAllyEntity> squadOf(ServerPlayer player) {
        UUID id = player.getUUID();
        return player.serverLevel().getEntities(ModEntities.ALIADO_ESPIRITUAL.get(),
                ally -> ally.isAlive() && !ally.isRemoved() && id.equals(ally.getOwnerUUID()));
    }

    /** Safety net, every second: no allies left, no timer. */
    public static void check(ServerPlayer player) {
        if (player.hasEffect(ModEffects.ALIADOS_ESPIRITUAIS.get()) && squadOf(player).isEmpty()) {
            player.removeEffect(ModEffects.ALIADOS_ESPIRITUAIS.get());
        }
    }

    public static void dissipateAllOf(ServerPlayer player) {
        for (SoulAllyEntity ally : squadOf(player)) {
            ally.dissipate();
        }
    }

    // ---------------------------------------------------------------- loyalty

    /** The summoner and everything on the summoner's side. */
    private boolean isFriendly(@Nullable Entity target) {
        if (target == this || (target instanceof LivingEntity living && this.isOwnedBy(living))) {
            return true;
        }
        return Allegiance.belongsTo(target, this.getOwner());
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

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity() != this && this.isFriendly(source.getEntity())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    // ---------------------------------------------------------------- misc

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

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
        return 0.55F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Spirits make no footsteps.
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
