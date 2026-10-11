package com.livrodassombras.entity;

import com.livrodassombras.registry.ModEffects;
import com.livrodassombras.registry.ModEntities;
import com.livrodassombras.summon.Allegiance;
import com.livrodassombras.summon.SpiritBound;
import com.livrodassombras.summon.SpiritSummoning;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Vex Espiritual: a Vex bound to whoever summoned it with the Livro dos Espíritos (two at a time).
 * <ul>
 *   <li>A little stronger than a Vex: 18 health (Vex 14) and 5 attack (Vex 4). Flies through walls like a Vex.</li>
 *   <li>Lives for 45 s; the summoner sees the remaining time as a status effect.</li>
 *   <li>Circles its summoner, attacks whatever the summoner attacks or whatever hurts the summoner,
 *       and goes after monsters that come close to the summoner.</li>
 *   <li>Never attacks its summoner's side, and its summoner's side can't hurt it.</li>
 * </ul>
 */
public class SoulVexEntity extends Vex implements SpiritBound {
    public static final int LIFETIME_TICKS = 20 * 45;
    public static final int SQUAD_SIZE = 2;
    private static final int OWNER_GRACE_TICKS = 20 * 5;
    private static final double LEASH = 16.0D;

    @Nullable
    private UUID summoner;
    private int lifeTicks = LIFETIME_TICKS;
    private int ownerMissingTicks;

    public SoulVexEntity(EntityType<? extends SoulVexEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vex.createAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** A new Vex for {@code owner}, not yet added to the level. */
    @Nullable
    public static SoulVexEntity create(ServerLevel level, Player owner) {
        SoulVexEntity vex = ModEntities.VEX_ESPIRITUAL.get().create(level);
        if (vex != null) {
            vex.summoner = owner.getUUID();
        }
        return vex;
    }

    @Override
    @Nullable
    public UUID getSummonerUUID() {
        return this.summoner;
    }

    @Override
    protected void registerGoals() {
        // Keep the Vex's own flying and charging, but none of its hostile targeting.
        super.registerGoals();
        this.targetSelector.removeAllGoals(goal -> true);
        this.targetSelector.addGoal(1, new SpiritGoals.DefendSummonerGoal(this));
        this.targetSelector.addGoal(2, new SpiritGoals.AssistSummonerGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, SpiritGoals.guard(this));
    }

    // ---------------------------------------------------------------- lifetime

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.4D), this.getRandomY(),
                        this.getRandomZ(0.4D), 0.0D, 0.01D, 0.0D);
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

        Player owner = SpiritGoals.summoner(this);
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

        // Its wandering is centred just above the summoner's head, so it circles them.
        this.setBoundOrigin(owner.blockPosition().above(2));
        if (this.getTarget() == null && this.distanceToSqr(owner) > LEASH * LEASH) {
            this.moveTo(owner.getX(), owner.getY() + 2.0D, owner.getZ(), this.getYRot(), this.getXRot());
            this.getMoveControl().setWantedPosition(owner.getX(), owner.getY() + 2.0D, owner.getZ(), 1.0D);
        }

        MobEffectInstance timer = owner.getEffect(ModEffects.VEX_ESPIRITUAIS.get());
        if (timer == null || timer.getDuration() < this.lifeTicks - 20) {
            owner.forceAddEffect(new MobEffectInstance(ModEffects.VEX_ESPIRITUAIS.get(), this.lifeTicks, 0, false, false, true), this);
        }
    }

    /** Ends the Vex in blue soul fire (time up, summoner gone). */
    public void dissipate() {
        if (this.level() instanceof ServerLevel level) {
            SpiritSummoning.vanish(level, this.getX(), this.getY() + 0.4D, this.getZ());
        }
        this.discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            SpiritSummoning.vanish(level, this.getX(), this.getY() + 0.4D, this.getZ());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!this.level().isClientSide && reason.shouldDestroy() && SpiritGoals.summoner(this) instanceof ServerPlayer owner
                && squadOf(owner).isEmpty()) {
            owner.removeEffect(ModEffects.VEX_ESPIRITUAIS.get());
            owner.displayClientMessage(Component.translatable("message.livro_das_sombras.vex_foram"), true);
        }
    }

    /** The player's living Vex in the player's level. */
    public static List<? extends SoulVexEntity> squadOf(ServerPlayer player) {
        UUID id = player.getUUID();
        return player.serverLevel().getEntities(ModEntities.VEX_ESPIRITUAL.get(),
                vex -> vex.isAlive() && !vex.isRemoved() && id.equals(vex.getSummonerUUID()));
    }

    /** Safety net, every second: no Vex left, no timer. */
    public static void check(ServerPlayer player) {
        if (player.hasEffect(ModEffects.VEX_ESPIRITUAIS.get()) && squadOf(player).isEmpty()) {
            player.removeEffect(ModEffects.VEX_ESPIRITUAIS.get());
        }
    }

    public static void dissipateAllOf(ServerPlayer player) {
        for (SoulVexEntity vex : squadOf(player)) {
            vex.dissipate();
        }
    }

    // ---------------------------------------------------------------- loyalty

    private boolean isFriendly(@Nullable Entity entity) {
        Player owner = SpiritGoals.summoner(this);
        if (owner == null) {
            return this.summoner != null && entity != null && this.summoner.equals(entity.getUUID());
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
            return false; // its own side can't hurt it (stray hits, area attacks)
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", this.lifeTicks);
        if (this.summoner != null) {
            tag.putUUID("Summoner", this.summoner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("LifeTicks")) {
            this.lifeTicks = tag.getInt("LifeTicks");
        }
        this.summoner = tag.hasUUID("Summoner") ? tag.getUUID("Summoner") : null;
    }
}
