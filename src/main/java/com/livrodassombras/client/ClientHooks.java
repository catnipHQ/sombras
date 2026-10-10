package com.livrodassombras.client;

import net.minecraft.client.Minecraft;

/** Client-only entry points. Only ever referenced through DistExecutor. */
public class ClientHooks {
    public static void openBloodBook() {
        Minecraft.getInstance().setScreen(new BloodBookScreen());
    }

    public static void openBeingMenu(int entityId) {
        Minecraft.getInstance().setScreen(new BloodBeingScreen(entityId));
    }
}
