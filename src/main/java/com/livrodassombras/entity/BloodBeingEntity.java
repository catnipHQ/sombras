package com.livrodassombras.entity;

import com.livrodassombras.client.ClientHooks;
import com.livrodassombras.registry.ModEffects;
import com.livrodassombras.summon.BloodBond;
import com.livrodassombras.summon.BloodPrice;
import java.util.UUID;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.joml.Vector3f;

/**
 * Ser de Sangue: a zombie-like servant summoned with the Livro do Sangue.
 * <ul>
 *   <li>Stronger than a zombie: 26 health (zombie 20), 5 attack (zombie 3), doesn't burn in daylight.</li>
 *   <li>Lives for 2 minutes; the summoner sees the remaining time as a status effect.</li>
 *   <li>Follows and defends its owner like a wolf: attacks whatever the owner attacks or whatever hurts the owner.</li>
 *   <li>Right-click toggles guard mode: it stays at its post, still defends, then returns to the post.</li>
 *   <li>Shift + right-click (owner only) opens the ritual menu: Prolongar (9.5 hearts and 2 max-health
 *       hearts until it dies, 10 minutes), Morra (dies at once) and Fortalecer (3 max-health hearts until it dies, for +5 damage and armor).</li>
 *   <li>Never attacks its owner, its owner's pets or its owner's other creatures.</li>
 * </ul>
 */
public class BloodBeingEntity extends TamableAnimal {
    public static final int LIFETIME_TICKS = 20 * 120; // 2 minutes
    /** How long the creature waits for an owner who logged off or changed dimension before dissipating. */
    private static final int OWNER_GRACE_TICKS = 20 * 5;

    /** Ritual: prolong. */
    public static final int PROLONG_TICKS = 20 * 60 * 10; // 10 minutes
    public static final float PROLONG_COST = 19.0F;       // 9.5 hearts

    /** Ritual: sacrifice. What the creature gains while the owner's 3 hearts are bound to it. */
    private static final UUID SACRIFICE_ID = UUID.fromString("c2d7a5e1-0b3f-4e62-9a8d-71f5e3b4c6a0");
    private static final double SACRIFICE_DAMAGE = 5.0D;
    private static final double SACRIFICE_ARMOR = 6.0D;
    private static final double SACRIFICE_TOUGHNESS = 2.0D;
    private static final double SACRIFICE_KNOCKBACK_RESISTANCE = 0.5D;

    /** Synced so the owner's ritual menu can show the remaining time and whether the sacrifice was made. */
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(BloodBeingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SACRIFICED =
            SynchedEntityData.defineId(BloodBeingEntity.class, EntityDataSerializers.BOOLEAN);

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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_LIFE, LIFETIME_TICKS);
        this.entityData.define(DATA_SACRIFICED, false);
    }

    /** Remaining lifetime in ticks (approximate on the client: synced once a second). */
    public int getLifeTicks() {
        return this.level().isClientSide ? this.entityData.get(DATA_LIFE) : this.lifeTicks;
    }

    public boolean isSacrificed() {
        return this.entityData.get(DATA_SACRIFICED);
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
        if (this.tickCount % 20 == 0) {
            this.entityData.set(DATA_LIFE, this.lifeTicks);
        }

        LivingEntity owner = this.getOwner();
        if (owner == null) {
            if (++this.ownerMissingTicks > OWNER_GRACE_TICKS) {
                this.dissipate();
            }
            return;
        }
        this.ownerMissingTicks = 0;
        // Dead owner, or the owner's link now points elsewhere (e.g. this creature sat in an unloaded chunk).
        if (!owner.isAlive() || (owner instanceof Player player && !BloodBond.isBoundTo(player, this))) {
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

    /** Releases the owner: timer effect removed, sacrificed hearts returned. */
    private void clearOwnerTimer() {
        if (this.getOwner() instanceof Player owner && BloodBond.isBoundTo(owner, this)) {
            BloodBond.release(owner);
        }
    }

    /** True if {@code player} already has a living Ser de Sangue (in any dimension). */
    public static boolean hasBeing(ServerLevel level, Player player) {
        return player instanceof ServerPlayer serverPlayer && BloodBond.find(serverPlayer) != null;
    }

    // ---------------------------------------------------------------- rituals (owner's menu)

    /**
     * Prolongar: 9.5 hearts, plus 2 max-health hearts until the creature dies (stacks each time),
     * for 10 more minutes of life (the timer is set to 10:00).
     */
    public void ritualProlong(Player player) {
        boolean creative = player.getAbilities().instabuild;
        if ((!creative && !BloodBond.canProlongCut(player)) || !BloodPrice.pay(player, PROLONG_COST)) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.ritual_sem_sangue"), true);
            return;
        }
        BloodBond.applyProlongCut(player);
        this.lifeTicks = PROLONG_TICKS;
        this.entityData.set(DATA_LIFE, this.lifeTicks);
        this.ritualEffects(1.0F);
        player.displayClientMessage(Component.translatable("message.livro_das_sombras.ritual_prolongado"), true);
    }

    /** Undo: the creature dies at once (its owner gets everything back as usual). */
    public void ritualUndo() {
        this.ritualEffects(0.5F);
        this.kill();
    }

    /** Sacrifice: 3 of the owner's max-health hearts, until the creature dies, for +5 damage and much more armor. */
    public void ritualSacrifice(Player player) {
        if (this.isSacrificed()) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.ritual_ja_feito"), true);
            return;
        }
        if (!BloodBond.canSacrifice(player)) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.ritual_sem_sangue"), true);
            return;
        }
        BloodBond.applySacrifice(player);
        addBuff(this, Attributes.ATTACK_DAMAGE, SACRIFICE_DAMAGE);
        addBuff(this, Attributes.ARMOR, SACRIFICE_ARMOR);
        addBuff(this, Attributes.ARMOR_TOUGHNESS, SACRIFICE_TOUGHNESS);
        addBuff(this, Attributes.KNOCKBACK_RESISTANCE, SACRIFICE_KNOCKBACK_RESISTANCE);
        this.entityData.set(DATA_SACRIFICED, true);
        this.ritualEffects(0.7F);
        player.displayClientMessage(Component.translatable("message.livro_das_sombras.ritual_sacrificio"), true);
    }

    private static void addBuff(LivingEntity entity, Attribute attribute, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.getModifier(SACRIFICE_ID) == null) {
            instance.addPermanentModifier(new AttributeModifier(SACRIFICE_ID, "Sacrifício de sangue",
                    amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private void ritualEffects(float pitch) {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(BLOOD, this.getX(), this.getY() + 1.0D, this.getZ(), 35, 0.35D, 0.8D, 0.35D, 0.02D);
            level.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.NEUTRAL, 0.7F, pitch);
        }
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
        // Shift + right-click: the ritual menu, only for the summoner.
        if (player.isShiftKeyDown()) {
            if (this.level().isClientSide) {
                int id = this.getId();
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openBeingMenu(id));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
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
        tag.putBoolean("Sacrificed", this.isSacrificed());
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
        this.entityData.set(DATA_LIFE, this.lifeTicks);
        this.entityData.set(DATA_SACRIFICED, tag.getBoolean("Sacrificed"));
        this.guarding = tag.getBoolean("Guarding");
        this.guardPos = tag.contains("GuardPos") ? NbtUtils.readBlockPos(tag.getCompound("GuardPos")) : null;
    }
}
