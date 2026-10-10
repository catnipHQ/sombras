package com.livrodassombras.client;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LivroDasSombras.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LANCA_DE_SANGUE.get(), BloodSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.SER_DE_SANGUE.get(), BloodBeingRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.SER_DE_SANGUE, BloodBeingModel::createBodyLayer);
    }
}
