package com.livrodassombras.effect;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

/**
 * Shown on the summoner while their Ser de Sangue is alive, so the inventory (E) displays its remaining time.
 * It does nothing by itself; the creature keeps it in sync and removes it when it dies.
 */
public class BloodBondEffect extends MobEffect {
    public BloodBondEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8A0303);
    }

    /** Milk doesn't remove it: it only mirrors the creature's lifetime. */
    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}
