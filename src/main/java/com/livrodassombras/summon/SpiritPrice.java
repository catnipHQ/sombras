package com.livrodassombras.summon;

import net.minecraft.world.entity.player.Player;

/**
 * Costs paid on the Spirit path: whole experience levels (the souls the player has gathered).
 * Creative players pay nothing.
 */
public final class SpiritPrice {
    private SpiritPrice() {}

    public static boolean canPay(Player player, int levels) {
        return player.getAbilities().instabuild || player.experienceLevel >= levels;
    }

    /** @return false if the player doesn't have enough levels (nothing is taken). */
    public static boolean pay(Player player, int levels) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (player.experienceLevel < levels) {
            return false;
        }
        player.giveExperienceLevels(-levels);
        return true;
    }
}
