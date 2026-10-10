package com.livrodassombras.client;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.registry.ModItems;
import com.livrodassombras.ritual.RitualBook;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client game events: the wheel key. */
@Mod.EventBusSubscriber(modid = LivroDasSombras.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        while (ModKeys.WHEEL.consumeClick()) {
            if (mc.player == null || mc.screen != null) {
                continue;
            }
            ItemStack book = heldBook();
            if (book.isEmpty()) {
                continue;
            }
            int section = RitualBook.section(book);
            if (section == RitualBook.SECTION_NONE) {
                mc.setScreen(new BloodBookRootScreen()); // nothing locked yet: pick a section first
            } else {
                mc.setScreen(new RadialScreen(section));
            }
        }
    }

    /** The Livro do Sangue in the local player's hand (main hand first), or empty. */
    static ItemStack heldBook() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return ItemStack.EMPTY;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = mc.player.getItemInHand(hand);
            if (stack.is(ModItems.LIVRO_DO_SANGUE.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0L : mc.level.getGameTime();
    }
}
