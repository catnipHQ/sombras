package com.livrodassombras;

import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.registry.ModCreativeTabs;
import com.livrodassombras.registry.ModEntities;
import com.livrodassombras.registry.ModItems;
import com.livrodassombras.registry.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LivroDasSombras.MOD_ID)
public class LivroDasSombras {
    public static final String MOD_ID = "livro_das_sombras";

    public LivroDasSombras() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
