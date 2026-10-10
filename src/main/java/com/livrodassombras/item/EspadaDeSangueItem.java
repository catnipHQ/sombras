package com.livrodassombras.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

/**
 * Espada de Sangue: identical to a diamond sword
 * (7 attack damage, -2.4 attack speed, 1561 durability, same enchantability).
 */
public class EspadaDeSangueItem extends SwordItem {
    public EspadaDeSangueItem(Properties properties) {
        super(Tiers.DIAMOND, 3, -2.4F, properties);
    }
}
