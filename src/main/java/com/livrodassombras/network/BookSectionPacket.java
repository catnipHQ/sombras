package com.livrodassombras.network;

import com.livrodassombras.registry.ModItems;
import com.livrodassombras.ritual.RitualBook;
import com.livrodassombras.ritual.RitualBooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: lock the ritual book in hand on a section (0 Invocações, 1 Rituais menores) or unlock it (-1). */
public record BookSectionPacket(int section) {

    public static void encode(BookSectionPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.section);
    }

    public static BookSectionPacket decode(FriendlyByteBuf buf) {
        return new BookSectionPacket(buf.readVarInt());
    }

    public static void handle(BookSectionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.section() < -1 || packet.section() > RitualBook.SECTION_RITUAIS) {
                return;
            }
            ItemStack book = RitualBooks.held(player);
            if (!book.isEmpty()) {
                RitualBook.setSection(book, packet.section());
            }
        });
        context.setPacketHandled(true);
    }
}
