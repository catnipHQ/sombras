package com.livrodassombras.ritual;

import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

/** The local player's Reverter Passos mark, as last sent by the server. */
public final class ClientRevertMark {
    @Nullable
    private static Vec3 spot;
    private static long ends;

    private ClientRevertMark() {}

    public static void set(boolean present, double x, double y, double z, long endTime) {
        spot = present ? new Vec3(x, y, z) : null;
        ends = endTime;
    }

    @Nullable
    public static Vec3 spot() {
        return spot;
    }

    public static long ends() {
        return ends;
    }
}
