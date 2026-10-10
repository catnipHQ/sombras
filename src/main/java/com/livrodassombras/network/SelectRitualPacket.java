package com.livrodassombras.network;

import com.livrodassombras.registry.ModItems;
import com.livrodassombras.ritual.MinorRitual;
import com.livrodassombras.ritual.RitualBook;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: the minor ritual chosen in the Livro do Sangue menu; it charges the book in hand. */
public record SelectRitualPacket(int index) {

    public static void encode(SelectRitualPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.index);
    }

    public static SelectRitualPacket decode(FriendlyByteBuf buf) {
        return new SelectRitualPacket(buf.readVarInt());
    }

    public static void handle(SelectRitualPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            MinorRitual ritual = MinorRitual.byIndex(packet.index());
            if (player == null || ritual == null) {
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack book = player.getItemInHand(hand);
                if (book.is(ModItems.LIVRO_DO_SANGUE.get())) {
                    RitualBook.charge(book, ritual);
                    player.displayClientMessage(Component.translatable("message.livro_das_sombras.ritual.preparado",
                            ritual.displayName()), true);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                            SoundSource.PLAYERS, 1.0F, 0.6F);
                    return;
                }
            }
        });
        context.setPacketHandled(true);
    }
}
