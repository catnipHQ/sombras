package com.livrodassombras.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A summoning book with a limited number of uses (its durability). Each successful summoning
 * spends one use and the book crumbles after the last. It can't be enchanted (no Unbreaking or
 * Mending) or repaired, so the only way to get another is a new ritual in the Caldeirão Ritualístico.
 */
public abstract class LimitedBookItem extends Item {
    public static final int USES = 5;

    protected LimitedBookItem(Properties properties) {
        super(properties.durability(USES));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairItem) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("tooltip.livro_das_sombras.usos", left, stack.getMaxDamage())
                .withStyle(ChatFormatting.DARK_RED));
    }
}
