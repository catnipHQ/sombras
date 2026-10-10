package com.livrodassombras.network;

import com.livrodassombras.ritual.ClientRitualCooldowns;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Server to client: when each ritual is ready again for this player (game time). */
public record RitualCooldownSyncPacket(Map<String, Long> ends) {

    public static void encode(RitualCooldownSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeMap(packet.ends, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeLong);
    }

    public static RitualCooldownSyncPacket decode(FriendlyByteBuf buf) {
        return new RitualCooldownSyncPacket(buf.readMap(HashMap::new, b -> b.readUtf(64), FriendlyByteBuf::readLong));
    }

    public static void handle(RitualCooldownSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientRitualCooldowns.set(packet.ends()));
        context.setPacketHandled(true);
    }
}
