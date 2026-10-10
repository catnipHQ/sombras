package com.livrodassombras.ritual;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.network.RitualCooldownSyncPacket;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Ritual cooldowns belong to the player (not the book): kept on the server in the player's data
 * as the game time each ritual is ready again, and copied to that player's client for the HUD and menus.
 */
@Mod.EventBusSubscriber(modid = LivroDasSombras.MOD_ID)
public final class RitualCooldowns {
    private static final String TAG = "veritates_ritual_cooldowns";

    private RitualCooldowns() {}

    public static long left(Player player, MinorRitual ritual) {
        CompoundTag tag = player.getPersistentData().getCompound(TAG);
        return Math.max(0L, tag.getLong(ritual.id()) - player.level().getGameTime());
    }

    public static void start(ServerPlayer player, MinorRitual ritual) {
        CompoundTag data = player.getPersistentData();
        CompoundTag tag = data.getCompound(TAG);
        tag.putLong(ritual.id(), player.level().getGameTime() + ritual.cooldownTicks());
        data.put(TAG, tag);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData().getCompound(TAG);
        Map<String, Long> ends = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            ends.put(key, tag.getLong(key));
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RitualCooldownSyncPacket(ends));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}
