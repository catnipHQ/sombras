package com.livrodassombras.summon;

import com.livrodassombras.entity.SoulAllyEntity;
import com.livrodassombras.entity.SoulGolemEntity;
import com.livrodassombras.entity.SoulPortalEntity;
import com.livrodassombras.entity.SoulVexEntity;
import com.livrodassombras.registry.ModSounds;
import com.livrodassombras.ritual.RitualCooldowns;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

/**
 * Summoning with the Livro dos Espíritos. Same rules as the other books (one group of each kind at a time,
 * one use of the book, 5 s on the book), but the price is experience levels, each summon has its own
 * cooldown, and the toll is 2 s of Darkness instead of Nausea.
 */
public final class SpiritSummoning {
    public static final int TOLL_TICKS = 40;

    private SpiritSummoning() {}

    public static boolean summon(ServerPlayer player, SpiritSummon summon, Item book) {
        ServerLevel level = player.serverLevel();

        long wait = RitualCooldowns.left(player, summon.id());
        if (wait > 0L) {
            warn(player, "message.livro_das_sombras.espiritos.espere", Mth.positiveCeilDiv((int) wait, 20));
            return false;
        }

        boolean present = switch (summon) {
            case VEX -> !SoulVexEntity.squadOf(player).isEmpty();
            case ALIADOS -> !SoulAllyEntity.squadOf(player).isEmpty();
            case GOLEM -> SpiritBond.find(player) != null;
        };
        if (present) {
            warn(player, "message.livro_das_sombras.ja_tem_" + summon.name().toLowerCase());
            return false;
        }

        if (!SpiritPrice.pay(player, summon.cost())) {
            warn(player, "message.livro_das_sombras.sem_almas");
            return false;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, TOLL_TICKS, 0));

        switch (summon) {
            case VEX -> {
                for (int i = 0; i < SoulVexEntity.SQUAD_SIZE; i++) {
                    SoulVexEntity vex = SoulVexEntity.create(level, player);
                    if (vex == null) {
                        continue;
                    }
                    double angle = player.getYRot() * Mth.DEG_TO_RAD + (i == 0 ? 0.0D : Math.PI);
                    double x = player.getX() + Math.cos(angle) * 1.2D;
                    double z = player.getZ() + Math.sin(angle) * 1.2D;
                    vex.moveTo(x, player.getY() + 1.6D, z, player.getYRot(), 0.0F);
                    vex.setBoundOrigin(player.blockPosition().above(2));
                    level.addFreshEntity(vex);
                    poof(level, x, player.getY() + 1.8D, z);
                }
                warn(player, "message.livro_das_sombras.vex_invocados");
            }
            case ALIADOS -> {
                for (int i = 0; i < SoulAllyEntity.SQUAD_SIZE; i++) {
                    SoulAllyEntity ally = SoulAllyEntity.create(level, player);
                    if (ally == null) {
                        continue;
                    }
                    double angle = player.getYRot() * Mth.DEG_TO_RAD + Math.PI / 2.0D + (i - 1) * 0.9D;
                    double x = player.getX() + Math.cos(angle) * 1.6D;
                    double z = player.getZ() + Math.sin(angle) * 1.6D;
                    ally.moveTo(x, player.getY() + 1.2D, z, player.getYRot(), 0.0F);
                    ally.setYHeadRot(player.getYRot());
                    level.addFreshEntity(ally);
                    poof(level, x, player.getY() + 1.4D, z);
                }
                warn(player, "message.livro_das_sombras.aliados_invocados");
            }
            case GOLEM -> {
                SoulGolemEntity golem = SoulGolemEntity.create(level, player);
                if (golem != null) {
                    Vec3 spot = SoulGolemEntity.spawnSpot(level, player, golem);
                    golem.moveTo(spot.x, player.getY(), spot.z, player.getYRot() + 180.0F, 0.0F);
                    golem.setYHeadRot(player.getYRot() + 180.0F);
                    level.addFreshEntity(golem);
                    SpiritBond.bind(player, golem);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spot.x, player.getY() + 1.4D, spot.z, 80, 0.6D, 1.2D, 0.6D, 0.04D);
                    level.sendParticles(ParticleTypes.SOUL, spot.x, player.getY() + 1.4D, spot.z, 30, 0.6D, 1.2D, 0.6D, 0.03D);
                    level.playSound(null, golem.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 1.0F, 0.6F);
                }
                warn(player, "message.livro_das_sombras.golem_invocado");
            }
        }

        summonEffects(level, player);
        if (!player.getAbilities().instabuild) {
            RitualCooldowns.start(player, summon.id(), summon.cooldownTicks());
        }
        BloodSummoning.spendBookUse(player, book);
        player.getCooldowns().addCooldown(book, BloodSummoning.COOLDOWN_TICKS);
        return true;
    }

    /** Whispering souls and a column of blue fire around the summoner. */
    public static void summonEffects(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.blockPosition(), ModSounds.SOUL_SUMMON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.blockPosition(), ModSounds.SOUL_WHISPER.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.5D, 0.8D, 0.5D, 0.02D);
        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 0.6D, player.getZ(), 15, 0.5D, 0.4D, 0.5D, 0.02D);
    }

    /** A puff of blue fire and a soul's sigh: a spirit appearing. */
    public static void poof(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 20, 0.3D, 0.4D, 0.3D, 0.03D);
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 6, 0.3D, 0.3D, 0.3D, 0.02D);
        level.playSound(null, x, y, z, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    /** A spirit leaving: souls rise out of a last flicker of blue fire. */
    public static void vanish(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 12, 0.3D, 0.5D, 0.3D, 0.04D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 15, 0.3D, 0.5D, 0.3D, 0.02D);
        level.playSound(null, x, y, z, SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 1.0F, 0.6F);
        level.playSound(null, x, y, z, ModSounds.SOUL_WHISPER.get(), SoundSource.NEUTRAL, 0.6F, 1.2F);
    }

    /** Every spirit the player summoned: on the player's death. */
    public static void dissipateAllOf(ServerPlayer player) {
        SoulVexEntity.dissipateAllOf(player);
        SoulAllyEntity.dissipateAllOf(player);
        SoulPortalEntity.closeAllOf(player.serverLevel(), player);
        SoulGolemEntity golem = SpiritBond.find(player);
        if (golem != null) {
            golem.dissipate();
        }
    }

    private static void warn(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }
}
