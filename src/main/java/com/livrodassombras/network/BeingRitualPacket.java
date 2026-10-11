package com.livrodassombras.network;

import com.livrodassombras.entity.BloodBeingEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: a ritual chosen in the Ser de Sangue menu. */
public record BeingRitualPacket(int entityId, int ritual) {
    public static final int PROLONG = 0;
    public static final int UNDO = 1;
    public static final int SACRIFICE = 2;

    private static final double MAX_DISTANCE_SQR = 10.0D * 10.0D;

    public static void encode(BeingRitualPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.ritual);
    }

    public static BeingRitualPacket decode(FriendlyByteBuf buf) {
        return new BeingRitualPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(BeingRitualPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            // Never trust the client: only the summoner, close by, on a living creature.
            if (!(player.level().getEntity(packet.entityId()) instanceof BloodBeingEntity being)
                    || !being.isAlive()
                    || !being.isOwnedBy(player)
                    || player.distanceToSqr(being) > MAX_DISTANCE_SQR) {
                return;
            }
            switch (packet.ritual()) {
                case PROLONG -> being.ritualProlong(player);
                case UNDO -> being.ritualUndo();
                case SACRIFICE -> being.ritualSacrifice(player);
                default -> { }
            }
        });
        context.setPacketHandled(true);
    }
}
