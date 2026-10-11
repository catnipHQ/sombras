package com.livrodassombras.network;

import com.livrodassombras.entity.SoulGolemEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: an option chosen in the Golem de Almas menu. */
public record GolemFormPacket(int entityId, int option) {
    public static final int COLOSSUS = 0;
    public static final int DESTROYER = 1;
    public static final int DIE = 2;

    private static final double MAX_DISTANCE_SQR = 10.0D * 10.0D;

    public static void encode(GolemFormPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.option);
    }

    public static GolemFormPacket decode(FriendlyByteBuf buf) {
        return new GolemFormPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(GolemFormPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            // Never trust the client: only the summoner, close by, on a living golem.
            if (!(player.level().getEntity(packet.entityId()) instanceof SoulGolemEntity golem)
                    || !golem.isAlive()
                    || !golem.isSummoner(player)
                    || player.distanceToSqr(golem) > MAX_DISTANCE_SQR) {
                return;
            }
            switch (packet.option()) {
                case COLOSSUS -> golem.toggleForm(player, SoulGolemEntity.Form.COLOSSUS);
                case DESTROYER -> golem.toggleForm(player, SoulGolemEntity.Form.DESTROYER);
                case DIE -> golem.ritualDie();
                default -> { }
            }
        });
        context.setPacketHandled(true);
    }
}
