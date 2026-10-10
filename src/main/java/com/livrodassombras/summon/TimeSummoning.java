package com.livrodassombras.summon;

import com.livrodassombras.entity.TimeSkeletonEntity;
import com.livrodassombras.registry.ModSounds;
import com.livrodassombras.ritual.RitualCooldowns;
import com.livrodassombras.ritual.TimeRituals;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Summoning with the Livro do Tempo. Same rules as the Livro do Sangue (one weapon at a time across both
 * books, a free slot, one use of the book, 5 s on the book), but the price is hunger, and the toll is
 * 2 s of Slowness instead of Nausea.
 */
public final class TimeSummoning {
    public static final int TOLL_TICKS = 40;

    private TimeSummoning() {}

    public static boolean summon(ServerPlayer player, TimeSummon summon, Item book) {
        ServerLevel level = player.serverLevel();

        long wait = RitualCooldowns.left(player, summon.id());
        if (wait > 0L) {
            warn(player, "message.livro_das_sombras.tempo.espere", Mth.positiveCeilDiv((int) wait, 20));
            return false;
        }

        if (summon == TimeSummon.ESQUELETOS) {
            if (!TimeSkeletonEntity.squadOf(player).isEmpty()) {
                warn(player, "message.livro_das_sombras.ja_tem_esqueletos");
                return false;
            }
        } else {
            if (BloodWeapons.hasAny(player)) {
                warn(player, "message.livro_das_sombras.ja_invocada");
                return false;
            }
            if (player.getInventory().getFreeSlot() == -1) {
                warn(player, "message.livro_das_sombras.inventario_cheio");
                return false;
            }
        }

        if (!TimePrice.pay(player, summon.cost())) {
            warn(player, "message.livro_das_sombras.sem_fome");
            return false;
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TOLL_TICKS, 0));

        if (summon == TimeSummon.ESQUELETOS) {
            for (int i = 0; i < TimeSkeletonEntity.SQUAD_SIZE; i++) {
                TimeSkeletonEntity skeleton = TimeSkeletonEntity.create(level, player);
                if (skeleton == null) {
                    continue;
                }
                double angle = player.getYRot() * Mth.DEG_TO_RAD + Math.PI / 2.0D + (i - 1) * 0.9D;
                double x = player.getX() + Math.cos(angle) * 1.6D;
                double z = player.getZ() + Math.sin(angle) * 1.6D;
                skeleton.moveTo(x, player.getY(), z, player.getYRot(), 0.0F);
                skeleton.setYHeadRot(player.getYRot());
                level.addFreshEntity(skeleton);
                poof(level, x, player.getY() + 0.4D, z);
            }
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.esqueletos_invocados"), true);
        } else {
            ItemStack stack = summon.icon();
            player.getInventory().add(stack);
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.invocada", stack.getHoverName()), true);
        }

        level.playSound(null, player.blockPosition(), ModSounds.TIME_SUMMON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.8F, 0.5F);
        level.sendParticles(TimeRituals.GREY, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.5D, 0.7D, 0.5D, 0.02D);
        level.sendParticles(TimeRituals.DARK, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.5D, 0.7D, 0.5D, 0.02D);

        BloodSummoning.spendBookUse(player, book);
        player.getCooldowns().addCooldown(book, BloodSummoning.COOLDOWN_TICKS);
        return true;
    }

    /** Grey puff and a dry tick: a time item or creature appearing or vanishing. */
    public static void poof(ServerLevel level, double x, double y, double z) {
        level.sendParticles(TimeRituals.GREY, x, y, z, 25, 0.3D, 0.3D, 0.3D, 0.02D);
        level.sendParticles(TimeRituals.DARK, x, y, z, 10, 0.3D, 0.3D, 0.3D, 0.02D);
        level.playSound(null, x, y, z, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.7F, 0.6F);
    }

    private static void warn(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }
}
