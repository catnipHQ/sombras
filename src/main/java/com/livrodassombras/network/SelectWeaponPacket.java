package com.livrodassombras.network;

import com.livrodassombras.registry.ModItems;
import com.livrodassombras.ritual.BookPath;
import com.livrodassombras.ritual.RitualBooks;
import com.livrodassombras.summon.BloodSummoning;
import com.livrodassombras.summon.TimeSummon;
import com.livrodassombras.summon.TimeSummoning;
import com.livrodassombras.summon.BloodWeapons;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client to server: the option chosen in a book's Invocações menu.
 * Livro do Tempo: indexes of TimeSummon. Livro do Sangue:
 * Indexes 0..n-1 are BloodWeapons.menuOrder(); index n (BloodWeapons.beingIndex()) is the Ser de Sangue.
 */
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
            BookPath path = BookPath.of(RitualBooks.held(player));
            if (path == null) {
                return;
            }
            Item book = path.book();
            if (player.getCooldowns().isOnCooldown(book)) {
                return;
            }

            if (path == BookPath.TIME) {
                TimeSummon summon = TimeSummon.byIndex(packet.index());
                if (summon != null) {
                    TimeSummoning.summon(player, summon, book);
                }
                return;
            }

            if (packet.index() == BloodWeapons.beingIndex()) {
                BloodSummoning.summonBeing(player, book);
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
