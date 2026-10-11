package com.livrodassombras.summon;

import com.livrodassombras.entity.BloodBeingEntity;
import com.livrodassombras.registry.ModEffects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * The link between a player and their Ser de Sangue.
 * <p>
 * The player remembers which creature is theirs (a UUID in their persistent data), and carries
 * the hearts they sacrificed as a max-health modifier. When the creature ends in any way, the
 * link is released: the timer effect goes away and the sacrificed hearts come back.
 * {@link #check} runs every second as a safety net for the cases the creature can't clean up
 * itself (owner offline or in another dimension, creature's chunk unloaded).
 */
public final class BloodBond {
    private static final String TAG = "veritates_ser_de_sangue";
    private static final UUID SACRIFICE_ID = UUID.fromString("5b1c3e2a-7d4f-4a8e-9c61-2f0d8b7a4e13");
    private static final UUID PROLONG_ID = UUID.fromString("8e2f4a61-3c7b-4d19-a0e5-6b9d1f2c7a48");

    /** 3 hearts. */
    public static final float SACRIFICE_HEALTH = 6.0F;

    /** 2 hearts of max health for each Prolongar, until the creature dies. */
    public static final float PROLONG_HEALTH = 4.0F;

    private BloodBond() {}

    public static void bind(Player player, BloodBeingEntity being) {
        player.getPersistentData().putUUID(TAG, being.getUUID());
    }

    public static boolean isBoundTo(Player player, Entity being) {
        CompoundTag data = player.getPersistentData();
        return data.hasUUID(TAG) && data.getUUID(TAG).equals(being.getUUID());
    }

    /** The player's living Ser de Sangue in any loaded dimension, or null. */
    @Nullable
    public static BloodBeingEntity find(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.hasUUID(TAG)) {
            return null;
        }
        UUID id = data.getUUID(TAG);
        for (ServerLevel level : player.server.getAllLevels()) {
            if (level.getEntity(id) instanceof BloodBeingEntity being && being.isAlive()) {
                return being;
            }
        }
        return null;
    }

    /** Ends the link: removes the timer effect and gives the sacrificed hearts back. */
    public static void release(Player player) {
        player.getPersistentData().remove(TAG);
        player.removeEffect(ModEffects.SER_DE_SANGUE.get());
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(SACRIFICE_ID);
            maxHealth.removeModifier(PROLONG_ID);
        }
    }

    public static boolean hasSacrificed(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        return maxHealth != null && maxHealth.getModifier(SACRIFICE_ID) != null;
    }

    public static boolean hasProlongCut(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        return maxHealth != null && maxHealth.getModifier(PROLONG_ID) != null;
    }

    /** The player must keep at least 1 heart of max health after Prolongar cuts 2 more. */
    public static boolean canProlongCut(Player player) {
        return player.getMaxHealth() - PROLONG_HEALTH >= 2.0F;
    }

    /** Takes 2 more hearts off the player's health bar (stacks with each Prolongar) until the link is released. */
    public static void applyProlongCut(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        AttributeModifier old = maxHealth.getModifier(PROLONG_ID);
        double total = (old == null ? 0.0D : old.getAmount()) - PROLONG_HEALTH;
        maxHealth.removeModifier(PROLONG_ID);
        maxHealth.addPermanentModifier(new AttributeModifier(PROLONG_ID, "Prolongar",
                total, AttributeModifier.Operation.ADDITION));
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** The player must keep at least 1 heart of max health after the sacrifice. */
    public static boolean canSacrifice(Player player) {
        return !hasSacrificed(player) && player.getMaxHealth() - SACRIFICE_HEALTH >= 2.0F;
    }

    /** Takes 3 hearts off the player's health bar until the link is released. */
    public static void applySacrifice(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null || maxHealth.getModifier(SACRIFICE_ID) != null) {
            return;
        }
        maxHealth.addPermanentModifier(new AttributeModifier(SACRIFICE_ID, "Sacrifício de sangue",
                -SACRIFICE_HEALTH, AttributeModifier.Operation.ADDITION));
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Safety net, every second: if the creature is gone, release whatever is still attached to the player. */
    public static void check(ServerPlayer player) {
        boolean linked = player.getPersistentData().hasUUID(TAG)
                || hasSacrificed(player)
                || hasProlongCut(player)
                || player.hasEffect(ModEffects.SER_DE_SANGUE.get());
        if (linked && find(player) == null) {
            release(player);
        }
    }
}
