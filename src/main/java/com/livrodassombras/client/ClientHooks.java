package com.livrodassombras.client;

import com.livrodassombras.ritual.BookPath;
import com.livrodassombras.ritual.RitualBook;
import net.minecraft.client.Minecraft;

/** Client-only entry points. Only ever referenced through DistExecutor. */
public class ClientHooks {
    public static void openBook(BookPath path, int section) {
        Minecraft.getInstance().setScreen(switch (section) {
            case RitualBook.SECTION_INVOCACOES -> new BloodBookScreen(path);
            case RitualBook.SECTION_RITUAIS -> new MinorRitualsScreen(path);
            default -> new BloodBookRootScreen(path);
        });
    }

    public static void openCauldronMenu(net.minecraft.core.BlockPos pos) {
        Minecraft.getInstance().setScreen(new RitualCauldronScreen(pos));
    }

    public static void openBeingMenu(int entityId) {
        Minecraft.getInstance().setScreen(new BloodBeingScreen(entityId));
    }

    public static void openGolemMenu(int entityId) {
        Minecraft.getInstance().setScreen(new SoulGolemScreen(entityId));
    }
}
