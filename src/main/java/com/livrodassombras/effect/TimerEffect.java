package com.livrodassombras.effect;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

/**
 * A status effect that only shows a summon's remaining time in the inventory (E).
 * It does nothing by itself, and milk doesn't remove it.
 */
public class TimerEffect extends MobEffect {
    public TimerEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}
