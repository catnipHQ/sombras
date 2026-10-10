package com.livrodassombras.network;

import com.livrodassombras.LivroDasSombras;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "3";

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
    }
}
