package com.livrodassombras.client;

import net.minecraft.client.Minecraft;

/** Client-only entry points. Only ever referenced through DistExecutor. */
public class ClientHooks {
    public static void openBloodBook() {
        Minecraft.getInstance().setScreen(new BloodBookRootScreen());
    }

    public static void openCauldronMenu(net.minecraft.core.BlockPos pos) {
        Minecraft.getInstance().setScreen(new RitualCauldronScreen(pos));
    }

    public static void openBeingMenu(int entityId) {
        Minecraft.getInstance().setScreen(new BloodBeingScreen(entityId));
    }
}
