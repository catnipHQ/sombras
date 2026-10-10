package com.livrodassombras.network;

import com.livrodassombras.registry.ModItems;
import com.livrodassombras.summon.BloodSummoning;
import com.livrodassombras.summon.BloodWeapons;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: the weapon chosen in the Livro do Sangue menu (index in BloodWeapons.menuOrder()). */
public record SelectWeaponPacket(int index) {

    public static void encode(SelectWeaponPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.index);
    }

    public static SelectWeaponPacket decode(FriendlyByteBuf buf) {
        return new SelectWeaponPacket(buf.readVarInt());
    }

    public static void handle(SelectWeaponPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            // Never trust the client: the player must really be holding the book and be off cooldown.
            Item book = ModItems.LIVRO_DO_SANGUE.get();
            if (!player.getMainHandItem().is(book) && !player.getOffhandItem().is(book)) {
                return;
            }
            if (player.getCooldowns().isOnCooldown(book)) {
                return;
            }

            Item weapon = BloodWeapons.byIndex(packet.index());
            if (weapon == null) {
                return;
            }

            BloodSummoning.summon(player, weapon, book);
        });
        context.setPacketHandled(true);
    }
}
