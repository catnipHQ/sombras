package com.livrodassombras.ritual;

import com.livrodassombras.LivroDasSombras;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Rituals that play out over several ticks (Dilacerar's delay and damage, the travelling slash). */
@Mod.EventBusSubscriber(modid = LivroDasSombras.MOD_ID)
public final class RitualTasks {
    /** One running ritual. Returns true from {@link #tick()} when it's over. */
    public interface Task {
        boolean tick();
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private RitualTasks() {}

    public static void add(Task task) {
        TASKS.add(task);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TASKS.isEmpty()) {
            return;
        }
        for (Task task : new ArrayList<>(TASKS)) {
            if (task.tick()) {
                TASKS.remove(task);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
    }
}
