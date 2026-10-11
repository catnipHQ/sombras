package com.livrodassombras.client;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.registry.ModItems;
import com.livrodassombras.ritual.BookPath;
import com.livrodassombras.ritual.RitualBook;
import com.livrodassombras.ritual.RitualBooks;
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
            BookPath path = BookPath.of(book);
            if (path == null) {
                continue;
            }
            int section = RitualBook.section(book);
            if (section == RitualBook.SECTION_NONE) {
                mc.setScreen(new BloodBookRootScreen(path)); // nothing locked yet: pick a section first
            } else {
                mc.setScreen(new RadialScreen(section, path));
            }
        }
    }

    /** The ritual book (Livro do Sangue or Livro do Tempo) in the local player's hand (main hand first), or empty. */
    static ItemStack heldBook() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? ItemStack.EMPTY : RitualBooks.held(mc.player);
    }

    static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0L : mc.level.getGameTime();
    }
}
