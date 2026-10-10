package com.livrodassombras.ritual;

import java.util.HashMap;
import java.util.Map;

/** The local player's ritual cooldowns, as last sent by the server (end game times). */
public final class ClientRitualCooldowns {
    private static final Map<String, Long> ENDS = new HashMap<>();

    private ClientRitualCooldowns() {}

    public static void set(Map<String, Long> ends) {
        ENDS.clear();
        ENDS.putAll(ends);
    }

    public static long left(MinorRitual ritual, long gameTime) {
        return Math.max(0L, ENDS.getOrDefault(ritual.id(), 0L) - gameTime);
    }

    /** 1 just after casting, 0 when ready. */
    public static float fraction(MinorRitual ritual, long gameTime) {
        return Math.min(1.0F, left(ritual, gameTime) / (float) ritual.cooldownTicks());
    }
}
