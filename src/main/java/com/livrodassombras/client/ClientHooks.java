package com.livrodassombras.client;

import net.minecraft.client.Minecraft;

/** Client-only entry points. Only ever referenced through DistExecutor. */
public class ClientHooks {
    public static void openBloodBook(int section) {
        Minecraft.getInstance().setScreen(switch (section) {
            case com.livrodassombras.ritual.RitualBook.SECTION_INVOCACOES -> new BloodBookScreen();
            case com.livrodassombras.ritual.RitualBook.SECTION_RITUAIS -> new MinorRitualsScreen();
            default -> new BloodBookRootScreen();
        });
    }

    public static void openCauldronMenu(net.minecraft.core.BlockPos pos) {
        Minecraft.getInstance().setScreen(new RitualCauldronScreen(pos));
    }

    public static void openBeingMenu(int entityId) {
        Minecraft.getInstance().setScreen(new BloodBeingScreen(entityId));
    }
}
