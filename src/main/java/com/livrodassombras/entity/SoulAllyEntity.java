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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Aliado Espiritual: a real Minecraft Allay, summoned three at a time by the Livro dos Espíritos.
 * <ul>
 *   <li>It is a vanilla Allay (same model, same flying, it follows its summoner like an Allay follows the
 *       player it likes), with 16 health instead of 10.</li>
 *   <li>Support, not damage: every second it saps the monsters near it with Weakness I (5 s) and Slowness I (4 s).</li>
 *   <li>Lives for 60 s; the summoner sees the remaining time as a status effect.</li>
 *   <li>Can't be handed items or duplicated, and its summoner's side can't hurt it.</li>
 * </ul>
 */
public class SoulAllyEntity extends Allay implements SpiritBound {
    public static final int LIFETIME_TICKS = 20 * 60;
    public static final int SQUAD_SIZE = 3;
    private static final int OWNER_GRACE_TICKS = 20 * 5;
    public static final int WEAKNESS_TICKS = 20 * 5;
    public static final int SLOWNESS_TICKS = 20 * 4;
    /** How far from the Allay a monster is sapped. */
    private static final double AURA_RADIUS = 6.0D;
    private static final int AURA_INTERVAL = 20;

    @Nullable
    private UUID summoner;
    private int lifeTicks = LIFETIME_TICKS;
    private int ownerMissingTicks;

    public SoulAllyEntity(EntityType<? extends SoulAllyEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Allay.createAttributes().add(Attributes.MAX_HEALTH, 16.0D);
    }

    /** A new Allay for {@code owner}, not yet added to the level. */
    @Nullable
    public static SoulAllyEntity create(ServerLevel level, Player owner) {
        SoulAllyEntity ally = ModEntities.ALIADO_ESPIRITUAL.get().create(level);
        if (ally != null) {
            ally.summoner = owner.getUUID();
            ally.getBrain().setMemory(MemoryModuleType.LIKED_PLAYER, owner.getUUID());
        }
        return ally;
    }

    @Override
    @Nullable
    public UUID getSummonerUUID() {
        return this.summoner;
    }

    // ---------------------------------------------------------------- lifetime and support

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.3D), this.getRandomY(),
                        this.getRandomZ(0.3D), 0.0D, 0.01D, 0.0D);
            }
            return;
        }
        if (!this.isAlive() || !(this.level() instanceof ServerLevel level)) {
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

        // An Allay follows the player it "likes": that is always its summoner.
        if (this.tickCount % 20 == 0) {
            this.getBrain().setMemory(MemoryModuleType.LIKED_PLAYER, owner.getUUID());
        }

        MobEffectInstance timer = owner.getEffect(ModEffects.ALIADOS_ESPIRITUAIS.get());
        if (timer == null || timer.getDuration() < this.lifeTicks - 20) {
            owner.forceAddEffect(new MobEffectInstance(ModEffects.ALIADOS_ESPIRITUAIS.get(), this.lifeTicks, 0, false, false, true), this);
        }

        if (this.tickCount % AURA_INTERVAL == 0) {
            this.sapEnemies(level);
        }
    }

    /** Weakness and Slowness on every monster close to this Allay (and to its summoner). */
    private void sapEnemies(ServerLevel level) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(AURA_RADIUS),
                e -> e.isAlive() && !e.isSpectator() && SpiritGoals.threatens(this, e))) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0), this);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_TICKS, 0), this);
            level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + target.getBbHeight() * 0.6D,
                    target.getZ(), 6, target.getBbWidth() * 0.3D, target.getBbHeight() * 0.25D,
                    target.getBbWidth() * 0.3D, 0.02D);
        }
    }

    /** Ends the ally in blue soul fire (time up, summoner gone). */
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
            owner.removeEffect(ModEffects.ALIADOS_ESPIRITUAIS.get());
            owner.displayClientMessage(Component.translatable("message.livro_das_sombras.aliados_foram"), true);
        }
    }

    /** The player's living allies in the player's level. */
    public static List<? extends SoulAllyEntity> squadOf(ServerPlayer player) {
        UUID id = player.getUUID();
        return player.serverLevel().getEntities(ModEntities.ALIADO_ESPIRITUAL.get(),
                ally -> ally.isAlive() && !ally.isRemoved() && id.equals(ally.getSummonerUUID()));
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

    private boolean isFriendly(@Nullable Entity entity) {
        Player owner = SpiritGoals.summoner(this);
        if (owner == null) {
            return this.summoner != null && entity != null && this.summoner.equals(entity.getUUID());
        }
        return Allegiance.sameSide(entity, owner);
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

    // ---------------------------------------------------------------- misc

    /** A spirit can't be handed items (the Allay would keep them) or duplicated with amethyst. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
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
