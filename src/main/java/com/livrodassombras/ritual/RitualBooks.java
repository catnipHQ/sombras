package com.livrodassombras.ritual;

import javax.annotation.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Finding the ritual book (Livro do Sangue or Livro do Tempo) a player is holding. */
public final class RitualBooks {
    private RitualBooks() {}

    /** The ritual book in the player's hand (main hand first), or null. */
    @Nullable
    public static InteractionHand handHolding(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (BookPath.of(player.getItemInHand(hand)) != null) {
                return hand;
            }
        }
        return null;
    }

    /** The ritual book in the player's hand, or an empty stack. */
    public static ItemStack held(Player player) {
        InteractionHand hand = handHolding(player);
        return hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
    }
}
