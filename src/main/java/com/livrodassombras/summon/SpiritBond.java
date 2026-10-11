package com.livrodassombras.summon;

import com.livrodassombras.entity.SoulGolemEntity;
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
 * The link between a player and their Golem de Almas (same idea as {@link BloodBond}).
 * <p>
 * The player remembers which golem is theirs, and carries the 5 hearts given up for the
 * Destroyer form as a max-health modifier. When the golem ends in any way, or leaves the form,
 * the hearts come back. {@link #check} runs every second as a safety net.
 */
public final class SpiritBond {
    private static final String TAG = "veritates_golem_de_almas";
    private static final UUID SACRIFICE_ID = UUID.fromString("3f6a9c2e-81b4-4d7a-b5e0-9c2d47f1a836");

    /** 5 hearts of max health while the golem is in Destroyer form. */
    public static final float SACRIFICE_HEALTH = 10.0F;

    private SpiritBond() {}

    public static void bind(Player player, SoulGolemEntity golem) {
        player.getPersistentData().putUUID(TAG, golem.getUUID());
    }

    public static boolean isBoundTo(Player player, Entity golem) {
        CompoundTag data = player.getPersistentData();
        return data.hasUUID(TAG) && data.getUUID(TAG).equals(golem.getUUID());
    }

    /** The player's living Golem de Almas in any loaded dimension, or null. */
    @Nullable
    public static SoulGolemEntity find(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.hasUUID(TAG)) {
            return null;
        }
        UUID id = data.getUUID(TAG);
        for (ServerLevel level : player.server.getAllLevels()) {
            if (level.getEntity(id) instanceof SoulGolemEntity golem && golem.isAlive()) {
                return golem;
            }
        }
        return null;
    }

    /** Ends the link and gives the hearts back. */
    public static void release(Player player) {
        player.getPersistentData().remove(TAG);
        releaseSacrifice(player);
    }

    /** Gives the Destroyer hearts back (the link stays). */
    public static void releaseSacrifice(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(SACRIFICE_ID);
        }
    }

    public static boolean hasSacrificed(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        return maxHealth != null && maxHealth.getModifier(SACRIFICE_ID) != null;
    }

    /** The player must keep at least 1 heart of max health after giving up 5. */
    public static boolean canSacrifice(Player player) {
        return !hasSacrificed(player) && player.getMaxHealth() - SACRIFICE_HEALTH >= 2.0F;
    }

    /** Takes 5 hearts off the player's health bar until the golem dies or leaves the Destroyer form. */
    public static void applySacrifice(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null || maxHealth.getModifier(SACRIFICE_ID) != null) {
            return;
        }
        maxHealth.addPermanentModifier(new AttributeModifier(SACRIFICE_ID, "Forma Destruidora",
                -SACRIFICE_HEALTH, AttributeModifier.Operation.ADDITION));
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Safety net, every second: golem gone, or no longer a Destroyer, means the hearts come back. */
    public static void check(ServerPlayer player) {
        boolean linked = player.getPersistentData().hasUUID(TAG) || hasSacrificed(player);
        if (!linked) {
            return;
        }
        SoulGolemEntity golem = find(player);
        if (golem == null) {
            release(player);
        } else if (hasSacrificed(player) && golem.getForm() != SoulGolemEntity.Form.DESTROYER) {
            releaseSacrifice(player);
        }
    }
}
