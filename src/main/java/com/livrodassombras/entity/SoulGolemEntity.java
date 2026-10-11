package com.livrodassombras.entity;

import com.livrodassombras.client.ClientHooks;
import com.livrodassombras.registry.ModEntities;
import com.livrodassombras.registry.ModSounds;
import com.livrodassombras.summon.Allegiance;
import com.livrodassombras.summon.SpiritBond;
import com.livrodassombras.summon.SpiritBound;
import com.livrodassombras.summon.SpiritSummoning;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Golem de Almas: a translucent iron golem of souls, summoned with the Livro dos Espíritos. One per player.
 * <ul>
 *   <li>A bit better than an Iron Golem: 120 health (100), 16 attack (15), 4 armor, a little faster,
 *       slowly heals itself out of combat. Doesn't burn.</li>
 *   <li>Stays until it dies. Follows and defends its summoner like the Ser de Sangue, and goes after monsters
 *       that come close to the summoner.</li>
 *   <li>Shift + right-click (summoner only) opens its menu:
 *     <ol>
 *       <li><b>Colosso</b>: no more normal punches; it charges for 1.5 s (arms rising, a ring of blue fire
 *           on the ground) and slams the ground, hitting every enemy within 5 blocks for 18 and throwing them up.</li>
 *       <li><b>Forma Destruidora</b>: the summoner gives up 5 hearts of max health until the golem dies (or leaves
 *           the form). The golem grows, gains +80 health, +8 armor, +4 toughness, +6 attack and Resistance I.</li>
 *       <li><b>Morra</b>: it dies at once.</li>
 *     </ol>
 *     The two forms exclude each other; choosing the active one again goes back to the normal form.</li>
 * </ul>
 */
public class SoulGolemEntity extends IronGolem implements SpiritBound {
    public enum Form {
        NORMAL, COLOSSUS, DESTROYER;

        static Form byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : NORMAL;
        }
    }

    private static final int OWNER_GRACE_TICKS = 20 * 5;

    // Colosso
    public static final int CHARGE_TICKS = 30;          // 1.5 s
    public static final double SLAM_RADIUS = 5.0D;
    public static final float SLAM_DAMAGE = 18.0F;
    private static final int SLAM_RECHARGE = 20 * 2;

    // Forma Destruidora
    private static final UUID DESTROYER_ID = UUID.fromString("a4d18e57-2b9c-4f31-8e6d-0c7b5f92e4a1");
    private static final double DESTROYER_HEALTH = 80.0D;
    private static final double DESTROYER_ARMOR = 8.0D;
    private static final double DESTROYER_TOUGHNESS = 4.0D;
    private static final double DESTROYER_DAMAGE = 6.0D;
    public static final float DESTROYER_SCALE = 1.25F;

    private static final EntityDataAccessor<Optional<UUID>> DATA_SUMMONER =
            SynchedEntityData.defineId(SoulGolemEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> DATA_FORM =
            SynchedEntityData.defineId(SoulGolemEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING =
            SynchedEntityData.defineId(SoulGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private int ownerMissingTicks;
    private int slamCooldown;
    /** Ticks since the charge started (server: drives the slam; client: drives the arms and the particles). */
    private int chargeTicks;

    public SoulGolemEntity(EntityType<? extends SoulGolemEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** A new golem for {@code owner}, not yet added to the level. */
    @Nullable
    public static SoulGolemEntity create(ServerLevel level, Player owner) {
        SoulGolemEntity golem = ModEntities.GOLEM_DE_ALMAS.get().create(level);
        if (golem != null) {
            golem.entityData.set(DATA_SUMMONER, Optional.of(owner.getUUID()));
        }
        return golem;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SUMMONER, Optional.empty());
        this.entityData.define(DATA_FORM, (byte) 0);
        this.entityData.define(DATA_CHARGING, false);
    }

    @Override
    @Nullable
    public UUID getSummonerUUID() {
        return this.entityData.get(DATA_SUMMONER).orElse(null);
    }

    public boolean isSummoner(Player player) {
        return player.getUUID().equals(this.getSummonerUUID());
    }

    public Form getForm() {
        return Form.byId(this.entityData.get(DATA_FORM));
    }

    /** 0 to 1 while charging a Colossus slam (for the arms and the particles), else 0. */
    public float chargeProgress(float partialTick) {
        if (!this.entityData.get(DATA_CHARGING)) {
            return 0.0F;
        }
        return Mth.clamp((this.chargeTicks + partialTick) / CHARGE_TICKS, 0.0F, 1.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SlamGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            public boolean canUse() {
                return SoulGolemEntity.this.getForm() != Form.COLOSSUS && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return SoulGolemEntity.this.getForm() != Form.COLOSSUS && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(3, new SpiritGoals.FollowSummonerGoal(this, 1.0D, 10.0F, 3.0F));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new SpiritGoals.DefendSummonerGoal(this));
        this.targetSelector.addGoal(2, new SpiritGoals.AssistSummonerGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, SpiritGoals.guard(this));
    }

    // ---------------------------------------------------------------- life

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.clientEffects();
            return;
        }
        if (!this.isAlive()) {
            return;
        }
        if (this.slamCooldown > 0) {
            this.slamCooldown--;
        }

        Player owner = SpiritGoals.summoner(this);
        if (owner == null) {
            if (++this.ownerMissingTicks > OWNER_GRACE_TICKS) {
                this.dissipate();
            }
            return;
        }
        this.ownerMissingTicks = 0;
        // Dead summoner, or the summoner's link now points elsewhere (this golem sat in an unloaded chunk).
        if (!owner.isAlive() || !SpiritBond.isBoundTo(owner, this)) {
            this.dissipate();
            return;
        }

        if (this.getForm() == Form.DESTROYER && this.tickCount % 100 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 220, 0, false, false));
        }
        // Slowly mends between fights.
        if (this.getTarget() == null && this.tickCount % 40 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }
    }

    private void clientEffects() {
        if (this.entityData.get(DATA_CHARGING)) {
            this.chargeTicks++;
        } else {
            this.chargeTicks = 0;
        }

        // Blue flames licking up from the feet.
        float scale = this.getForm() == Form.DESTROYER ? DESTROYER_SCALE : 1.0F;
        for (int i = 0; i < 2; i++) {
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.6D), this.getY() + this.random.nextDouble() * 0.4D * scale,
                        this.getRandomZ(0.6D), 0.0D, 0.02D, 0.0D);
            }
        }

        float progress = this.chargeProgress(0.0F);
        if (progress <= 0.0F) {
            return;
        }
        // Charging: a ring of blue fire marks the area, souls spiral up into the raised fists.
        double r = SLAM_RADIUS;
        double spin = this.tickCount * 0.25D;
        for (int i = 0; i < 6; i++) {
            double a = spin + i * Math.PI / 3.0D;
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX() + Math.cos(a) * r, this.getY() + 0.1D,
                    this.getZ() + Math.sin(a) * r, 0.0D, 0.01D, 0.0D);
        }
        double inner = (1.0D - progress) * 2.5D + 0.4D;
        double top = this.getBbHeight() * (0.6D + 0.5D * progress);
        for (int i = 0; i < 3; i++) {
            double a = -spin * 1.6D + i * Math.PI * 2.0D / 3.0D;
            this.level().addParticle(ParticleTypes.SOUL, this.getX() + Math.cos(a) * inner, this.getY() + top * progress,
                    this.getZ() + Math.sin(a) * inner, 0.0D, 0.04D, 0.0D);
        }
        if (progress > 0.7F && this.random.nextInt(2) == 0) {
            this.level().addParticle(ParticleTypes.SCULK_SOUL, this.getRandomX(0.8D), this.getY() + this.getBbHeight(),
                    this.getRandomZ(0.8D), 0.0D, 0.05D, 0.0D);
        }
    }

    /** Ends the golem in blue soul fire (summoner gone). */
    public void dissipate() {
        if (this.level() instanceof ServerLevel level) {
            SpiritSummoning.vanish(level, this.getX(), this.getY() + 1.4D, this.getZ());
            if (SpiritGoals.summoner(this) instanceof Player owner) {
                owner.displayClientMessage(Component.translatable("message.livro_das_sombras.golem_dissipado"), true);
            }
        }
        this.discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.releaseOwner();
        if (this.level() instanceof ServerLevel level) {
            SpiritSummoning.vanish(level, this.getX(), this.getY() + 1.4D, this.getZ());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            this.releaseOwner();
        }
        super.remove(reason);
    }

    /** The summoner gets the Destroyer hearts back and is free to summon another golem. */
    private void releaseOwner() {
        if (SpiritGoals.summoner(this) instanceof Player owner && SpiritBond.isBoundTo(owner, this)) {
            SpiritBond.release(owner);
        }
    }

    // ---------------------------------------------------------------- menu and forms

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !this.isSummoner(player)) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide) {
            int id = this.getId();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openGolemMenu(id));
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /** Menu choice: the given form, or back to normal if it's the one already active. */
    public void toggleForm(Player player, Form form) {
        Form next = this.getForm() == form ? Form.NORMAL : form;
        if (next == Form.DESTROYER && !player.getAbilities().instabuild && !SpiritBond.canSacrifice(player)) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.golem.sem_vida"), true);
            return;
        }
        this.setForm(next, player);
        player.displayClientMessage(Component.translatable("message.livro_das_sombras.golem.forma." + next.name().toLowerCase()), true);
    }

    private void setForm(Form form, Player owner) {
        Form old = this.getForm();
        if (old == form) {
            return;
        }
        if (old == Form.DESTROYER) {
            removeBuff(this, Attributes.MAX_HEALTH);
            removeBuff(this, Attributes.ARMOR);
            removeBuff(this, Attributes.ARMOR_TOUGHNESS);
            removeBuff(this, Attributes.ATTACK_DAMAGE);
            this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            if (this.getHealth() > this.getMaxHealth()) {
                this.setHealth(this.getMaxHealth());
            }
            SpiritBond.releaseSacrifice(owner);
        }
        if (form == Form.DESTROYER) {
            if (SpiritBond.canSacrifice(owner)) {
                SpiritBond.applySacrifice(owner);
            }
            addBuff(this, Attributes.MAX_HEALTH, DESTROYER_HEALTH);
            addBuff(this, Attributes.ARMOR, DESTROYER_ARMOR);
            addBuff(this, Attributes.ARMOR_TOUGHNESS, DESTROYER_TOUGHNESS);
            addBuff(this, Attributes.ATTACK_DAMAGE, DESTROYER_DAMAGE);
            this.heal((float) DESTROYER_HEALTH);
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 220, 0, false, false));
        }
        this.entityData.set(DATA_FORM, (byte) form.ordinal());
        this.entityData.set(DATA_CHARGING, false);
        this.chargeTicks = 0;
        this.refreshDimensions();

        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.4D, this.getZ(), 60, 0.6D, 1.0D, 0.6D, 0.05D);
            level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.4D, this.getZ(), 25, 0.6D, 1.0D, 0.6D, 0.03D);
            level.playSound(null, this.blockPosition(), ModSounds.SOUL_SUMMON.get(), SoundSource.NEUTRAL, 1.0F,
                    form == Form.DESTROYER ? 0.6F : 1.0F);
            level.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.NEUTRAL, 0.8F,
                    form == Form.NORMAL ? 1.4F : 0.7F);
        }
    }

    /** Morra: dies at once. */
    public void ritualDie() {
        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 1.2F, 0.5F);
        }
        this.kill();
    }

    private static void addBuff(LivingEntity entity, Attribute attribute, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.getModifier(DESTROYER_ID) == null) {
            instance.addPermanentModifier(new AttributeModifier(DESTROYER_ID, "Forma Destruidora",
                    amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void removeBuff(LivingEntity entity, Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(DESTROYER_ID);
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        EntityDimensions size = super.getDimensions(pose);
        return this.getForm() == Form.DESTROYER ? size.scale(DESTROYER_SCALE) : size;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_FORM.equals(key)) {
            this.refreshDimensions();
        }
    }

    // ---------------------------------------------------------------- Colosso

    /** In Colossus form: walk up to the target, charge, slam. Replaces the normal punches. */
    private class SlamGoal extends Goal {
        private int pathDelay;

        SlamGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private SoulGolemEntity golem() {
            return SoulGolemEntity.this;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.golem().getTarget();
            return this.golem().getForm() == Form.COLOSSUS && target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() || this.golem().chargeTicks > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.pathDelay = 0;
            this.golem().setAggressive(true);
        }

        @Override
        public void stop() {
            SoulGolemEntity golem = this.golem();
            golem.setAggressive(false);
            golem.chargeTicks = 0;
            golem.entityData.set(DATA_CHARGING, false);
        }

        @Override
        public void tick() {
            SoulGolemEntity golem = this.golem();
            LivingEntity target = golem.getTarget();

            if (golem.chargeTicks > 0) {
                golem.getNavigation().stop();
                if (target != null) {
                    golem.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
                if (++golem.chargeTicks >= CHARGE_TICKS) {
                    golem.slam();
                    golem.chargeTicks = 0;
                    golem.entityData.set(DATA_CHARGING, false);
                    golem.slamCooldown = SLAM_RECHARGE;
                }
                return;
            }
            if (target == null) {
                return;
            }

            golem.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double reach = SLAM_RADIUS - 1.5D + target.getBbWidth() / 2.0D;
            if (golem.distanceToSqr(target) > reach * reach) {
                if (--this.pathDelay <= 0) {
                    this.pathDelay = 10;
                    golem.getNavigation().moveTo(target, 1.0D);
                }
            } else if (golem.slamCooldown <= 0) {
                golem.getNavigation().stop();
                golem.chargeTicks = 1;
                golem.entityData.set(DATA_CHARGING, true);
                golem.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.2F, 0.7F);
                golem.playSound(ModSounds.SOUL_SUMMON.get(), 1.0F, 0.6F);
            } else {
                golem.getNavigation().stop();
            }
        }
    }

    /** The Colossus slam: every enemy within 5 blocks takes 18 damage and is thrown up and away. */
    private void slam() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Player owner = SpiritGoals.summoner(this);
        LivingEntity target = this.getTarget();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SLAM_RADIUS, 2.0D, SLAM_RADIUS), e -> e != this && e.isAlive() && !e.isSpectator())) {
            double dx = victim.getX() - this.getX();
            double dz = victim.getZ() - this.getZ();
            if (dx * dx + dz * dz > SLAM_RADIUS * SLAM_RADIUS || !this.isEnemy(victim, target, owner)) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), SLAM_DAMAGE)) {
                double len = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
                victim.setDeltaMovement(victim.getDeltaMovement().add(dx / len * 0.6D, 0.55D, dz / len * 0.6D));
                victim.hurtMarked = true;
            }
        }

        // A wave of blue fire rolling outwards, and a burst of souls.
        for (int ring = 1; ring <= 5; ring++) {
            double r = SLAM_RADIUS * ring / 5.0D;
            int count = 10 + ring * 6;
            for (int i = 0; i < count; i++) {
                double a = i * Math.PI * 2.0D / count;
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX() + Math.cos(a) * r, this.getY() + 0.15D,
                        this.getZ() + Math.sin(a) * r, 1, 0.0D, 0.05D, 0.0D, 0.02D);
            }
        }
        level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.5D, this.getZ(), 40, 1.5D, 0.3D, 1.5D, 0.06D);
        level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.3D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.broadcastEntityEvent(this, (byte) 4); // the swing of both arms
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 0.7F, 1.3F);
        level.playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.NEUTRAL, 1.2F, 0.6F);
        level.playSound(null, this.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 1.5F, 0.7F);
    }

    /** The slam spares the summoner's side and bystanders: it hits monsters, its target, and whatever is after its side. */
    private boolean isEnemy(LivingEntity victim, @Nullable LivingEntity target, @Nullable Player owner) {
        if (this.isFriendly(victim)) {
            return false;
        }
        if (victim == target || victim instanceof Enemy) {
            return true;
        }
        return victim instanceof Mob mob && mob.getTarget() != null
                && (mob.getTarget() == this || (owner != null && Allegiance.sameSide(mob.getTarget(), owner)));
    }

    // ---------------------------------------------------------------- loyalty

    private boolean isFriendly(@Nullable Entity entity) {
        Player owner = SpiritGoals.summoner(this);
        if (owner == null) {
            UUID id = this.getSummonerUUID();
            return id != null && entity != null && id.equals(entity.getUUID());
        }
        return Allegiance.sameSide(entity, owner);
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
    public boolean isAlliedTo(Entity entity) {
        return this.isFriendly(entity) || super.isAlliedTo(entity);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity() != this && this.isFriendly(source.getEntity())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID id = this.getSummonerUUID();
        if (id != null) {
            tag.putUUID("Summoner", id);
        }
        tag.putByte("Form", (byte) this.getForm().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_SUMMONER, tag.hasUUID("Summoner") ? Optional.of(tag.getUUID("Summoner")) : Optional.empty());
        this.entityData.set(DATA_FORM, tag.getByte("Form"));
        this.refreshDimensions();
    }

    /** Where a new golem stands: two blocks in front of the summoner if there's room, else on the summoner. */
    public static Vec3 spawnSpot(ServerLevel level, Player owner, SoulGolemEntity golem) {
        Vec3 look = Vec3.directionFromRotation(0.0F, owner.getYRot());
        Vec3 front = owner.position().add(look.scale(2.2D));
        golem.moveTo(front.x, owner.getY(), front.z, owner.getYRot() + 180.0F, 0.0F);
        return level.noCollision(golem) ? front : owner.position();
    }
}
