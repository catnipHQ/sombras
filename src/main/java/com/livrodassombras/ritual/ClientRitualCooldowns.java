package com.livrodassombras.ritual;

import java.util.HashMap;
import java.util.Map;

/** The local player's ritual (and summon) cooldowns, as last sent by the server (end game times). */
public final class ClientRitualCooldowns {
    private static final Map<String, Long> ENDS = new HashMap<>();

    private ClientRitualCooldowns() {}

    public static void set(Map<String, Long> ends) {
        ENDS.clear();
        ENDS.putAll(ends);
    }

    public static long left(String id, long gameTime) {
        return Math.max(0L, ENDS.getOrDefault(id, 0L) - gameTime);
    }

    public static long left(MinorRitual ritual, long gameTime) {
        return left(ritual.id(), gameTime);
    }

    /** 1 just after starting, 0 when ready. */
    public static float fraction(String id, int totalTicks, long gameTime) {
        return totalTicks <= 0 ? 0.0F : Math.min(1.0F, left(id, gameTime) / (float) totalTicks);
    }

    public static float fraction(MinorRitual ritual, long gameTime) {
        return fraction(ritual.id(), ritual.cooldownTicks(), gameTime);
    }
}
