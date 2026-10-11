package com.livrodassombras;

import com.livrodassombras.entity.BloodBeingEntity;
import com.livrodassombras.entity.SoulAllyEntity;
import com.livrodassombras.entity.SoulGolemEntity;
import com.livrodassombras.entity.SoulVexEntity;
import com.livrodassombras.entity.TimeSkeletonEntity;
import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.registry.ModBlockEntities;
import com.livrodassombras.registry.ModBlocks;
import com.livrodassombras.registry.ModCreativeTabs;
import com.livrodassombras.registry.ModEffects;
import com.livrodassombras.registry.ModEntities;
import com.livrodassombras.registry.ModItems;
import com.livrodassombras.registry.ModParticles;
import com.livrodassombras.registry.ModSounds;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LivroDasSombras.MOD_ID)
public class LivroDasSombras {
    public static final String MOD_ID = "livro_das_sombras";

    public LivroDasSombras() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerAttributes);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SER_DE_SANGUE.get(), BloodBeingEntity.createAttributes().build());
        event.put(ModEntities.ESQUELETO_DO_TEMPO.get(), TimeSkeletonEntity.createAttributes().build());
        event.put(ModEntities.VEX_ESPIRITUAL.get(), SoulVexEntity.createAttributes().build());
        event.put(ModEntities.ALIADO_ESPIRITUAL.get(), SoulAllyEntity.createAttributes().build());
        event.put(ModEntities.GOLEM_DE_ALMAS.get(), SoulGolemEntity.createAttributes().build());
    }
}
