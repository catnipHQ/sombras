package com.livrodassombras.summon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/**
 * Hunger costs paid by the player on the Time path (2 points = 1 drumstick).
 * Saturation goes first, like with any exertion; creative players pay nothing.
 */
public final class TimePrice {
    private TimePrice() {}

    public static boolean canPay(Player player, int amount) {
        return player.getAbilities().instabuild || player.getFoodData().getFoodLevel() >= amount;
    }

    /** @return false if the player is too hungry (nothing is taken). */
    public static boolean pay(Player player, int amount) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        FoodData food = player.getFoodData();
        if (food.getFoodLevel() < amount) {
            return false;
        }
        food.setFoodLevel(food.getFoodLevel() - amount);
        food.setSaturation(Math.min(food.getSaturationLevel(), food.getFoodLevel()));
        return true;
    }
}
