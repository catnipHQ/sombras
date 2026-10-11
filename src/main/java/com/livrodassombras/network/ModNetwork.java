package com.livrodassombras.network;

import com.livrodassombras.LivroDasSombras;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "7";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LivroDasSombras.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, SelectWeaponPacket.class,
                SelectWeaponPacket::encode, SelectWeaponPacket::decode, SelectWeaponPacket::handle);
        CHANNEL.registerMessage(1, BeingRitualPacket.class,
                BeingRitualPacket::encode, BeingRitualPacket::decode, BeingRitualPacket::handle);
        CHANNEL.registerMessage(2, CauldronChoicePacket.class,
                CauldronChoicePacket::encode, CauldronChoicePacket::decode, CauldronChoicePacket::handle);
        CHANNEL.registerMessage(3, SelectRitualPacket.class,
                SelectRitualPacket::encode, SelectRitualPacket::decode, SelectRitualPacket::handle);
        CHANNEL.registerMessage(4, BookSectionPacket.class,
                BookSectionPacket::encode, BookSectionPacket::decode, BookSectionPacket::handle);
        CHANNEL.registerMessage(5, RitualCooldownSyncPacket.class,
                RitualCooldownSyncPacket::encode, RitualCooldownSyncPacket::decode, RitualCooldownSyncPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(6, RevertMarkSyncPacket.class,
                RevertMarkSyncPacket::encode, RevertMarkSyncPacket::decode, RevertMarkSyncPacket::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(7, GolemFormPacket.class,
                GolemFormPacket::encode, GolemFormPacket::decode, GolemFormPacket::handle);
    }
}
