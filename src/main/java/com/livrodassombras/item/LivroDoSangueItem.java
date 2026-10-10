package com.livrodassombras.item;

import com.livrodassombras.client.ClientHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Upgraded Livro das Sombras. Right-click opens a menu to choose which weapon to summon;
 * the choice is sent to the server, which applies the same rules as the Livro das Sombras.
 */
public class LivroDoSangueItem extends Item {
    public LivroDoSangueItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openBloodBook);
        }
        return InteractionResultHolder.consume(book);
    }
}
