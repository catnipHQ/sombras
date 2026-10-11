package com.livrodassombras.network;

import com.livrodassombras.ritual.ClientRevertMark;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Server to client: the player's Reverter Passos mark (or that there is none), for the HUD. */
public record RevertMarkSyncPacket(boolean present, double x, double y, double z, long ends) {

    public static void encode(RevertMarkSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.present);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeLong(packet.ends);
    }

    public static RevertMarkSyncPacket decode(FriendlyByteBuf buf) {
        return new RevertMarkSyncPacket(buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readLong());
    }

    public static void handle(RevertMarkSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientRevertMark.set(packet.present(), packet.x(), packet.y(), packet.z(), packet.ends()));
        context.setPacketHandled(true);
    }
}
