package com.livrodassombras.summon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Health costs paid by the player (summoning, bow shots, hammer slams).
 * A price is never paid if it would kill the player, and creative players pay nothing.
 */
public final class BloodPrice {
    private BloodPrice() {}

    public static boolean canPay(Player player, float amount) {
        return player.getAbilities().instabuild || player.getHealth() > amount;
    }

    /**
     * Takes {@code amount} health from the player as magic damage. Server side only.
     *
     * @return false if the player doesn't have enough health (nothing is taken)
     */
    public static boolean pay(Player player, float amount) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (player.getHealth() <= amount) {
            return false;
        }
        // Clear the hurt cooldown so the price is always charged, even right after taking a hit.
        player.invulnerableTime = 0;
        return player.hurt(player.damageSources().magic(), amount);
    }

    /** Action-bar warning shown when a weapon can't be used for lack of blood. */
    public static void warnNotEnough(Player player) {
        player.displayClientMessage(Component.translatable("message.livro_das_sombras.sem_sangue_arma"), true);
    }
}
