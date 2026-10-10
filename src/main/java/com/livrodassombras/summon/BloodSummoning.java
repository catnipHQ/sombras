package com.livrodassombras.summon;

import com.livrodassombras.entity.BloodBeingEntity;
import com.livrodassombras.entity.BloodSpearEntity;
import com.livrodassombras.registry.ModEntities;
import com.livrodassombras.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/**
 * Shared summoning rules for every book: blood price, nausea, one weapon at a time,
 * a free inventory slot, one Ser de Sangue at a time, effects and cooldown.
 */
public final class BloodSummoning {
    public static final int COOLDOWN_TICKS = 20 * 5; // 5 seconds
    public static final float BLOOD_COST = 10.0F;    // 10 health points = 5 hearts
    public static final int NAUSEA_TICKS = 40;       // 2 seconds

    private BloodSummoning() {}

    /**
     * Tries to summon {@code weapon} for the player. Server side only.
     *
     * @param book the book item used, which receives the cooldown
     * @return true if the weapon was summoned
     */
    public static boolean summon(Player player, Item weapon, Item book) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        // Only one weapon at a time.
        if (BloodWeapons.hasAny(player)) {
            warn(player, "message.livro_das_sombras.ja_invocada");
            return false;
        }

        // Dropped weapons vanish, so there must be room for it in the inventory.
        if (player.getInventory().getFreeSlot() == -1) {
            warn(player, "message.livro_das_sombras.inventario_cheio");
            return false;
        }

        if (!payBloodPrice(player)) {
            return false;
        }

        // A spear that is still flying around from before is replaced by the new weapon.
        discardThrownSpears(level, player);

        player.getInventory().add(new ItemStack(weapon));

        summonEffects(level, player);
        spendBookUse(player, book);
        player.getCooldowns().addCooldown(book, COOLDOWN_TICKS);
        player.displayClientMessage(
                Component.translatable("message.livro_das_sombras.invocada", weapon.getDescription()), true);
        return true;
    }

    /**
     * Tries to summon a Ser de Sangue next to the player. Server side only.
     *
     * @return true if the creature was summoned
     */
    public static boolean summonBeing(Player player, Item book) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        if (BloodBeingEntity.hasBeing(level, player)) {
            warn(player, "message.livro_das_sombras.ja_tem_ser");
            return false;
        }

        BloodBeingEntity being = ModEntities.SER_DE_SANGUE.get().create(level);
        if (being == null || !payBloodPrice(player)) {
            return false;
        }

        being.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        being.setYHeadRot(player.getYRot());
        being.setTame(true);
        being.setOwnerUUID(player.getUUID());
        level.addFreshEntity(being);
        BloodBond.bind(player, being);

        summonEffects(level, player);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.0F), 1.4F),
                being.getX(), being.getY() + 1.0, being.getZ(), 60, 0.4, 0.9, 0.4, 0.02);

        spendBookUse(player, book);
        player.getCooldowns().addCooldown(book, COOLDOWN_TICKS);
        player.displayClientMessage(Component.translatable("message.livro_das_sombras.ser_invocado"), true);
        return true;
    }

    /** One use of the book in the player's hand is spent; it crumbles after the last (creative players spend nothing). */
    public static void spendBookUse(Player player, Item book) {
        for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(book)) {
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                return;
            }
        }
    }

    /** Charges 5 hearts and applies Nausea. The summoning must never kill the player. */
    private static boolean payBloodPrice(Player player) {
        if (!BloodPrice.pay(player, BLOOD_COST)) {
            warn(player, "message.livro_das_sombras.sem_sangue");
            return false;
        }
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS));
        return true;
    }

    /** Blood magic sound (two layers) and red particles. */
    private static void summonEffects(ServerLevel level, Player player) {
        level.playSound(null, player.blockPosition(), ModSounds.BLOOD_SUMMON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.blockPosition(), ModSounds.BLOOD_SQUELCH.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.0F), 1.2F),
                player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 0.7, 0.5, 0.02);
    }

    private static void discardThrownSpears(ServerLevel level, Player player) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof BloodSpearEntity spear && spear.getOwner() == player) {
                spear.discard();
            }
        }
    }

    private static void warn(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
