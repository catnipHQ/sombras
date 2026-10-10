package com.livrodassombras.item;

import com.livrodassombras.registry.ModItems;
import com.livrodassombras.summon.BloodSummoning;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Right-click to summon the Espada de Sangue (rules and cost live in BloodSummoning). */
public class LivroDasSombrasItem extends Item {
    public LivroDasSombrasItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(book, true);
        }

        boolean summoned = BloodSummoning.summon(player, ModItems.ESPADA_DE_SANGUE.get(), this);
        return summoned ? InteractionResultHolder.sidedSuccess(book, false) : InteractionResultHolder.fail(book);
    }
}
