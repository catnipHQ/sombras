package com.livrodassombras.item;

import com.livrodassombras.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

/**
 * Espada de Sangue: a diamond sword (7 attack damage, -2.4 attack speed, 1561 durability,
 * same enchantability) whose hits cause a minor bleed: Hemorragia for 4 seconds,
 * half a heart per second and a blood trail behind the target. Hits refresh it; it doesn't stack.
 */
public class EspadaDeSangueItem extends SwordItem {
    public static final int BLEED_TICKS = 20 * 4;

    public EspadaDeSangueItem(Properties properties) {
        super(Tiers.DIAMOND, 3, -2.4F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide) {
            target.addEffect(new MobEffectInstance(ModEffects.HEMORRAGIA.get(), BLEED_TICKS, 0, false, false, true), attacker);
        }
        return super.hurtEnemy(stack, target, attacker);
    }
}
