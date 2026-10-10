package com.livrodassombras.client;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.block.RitualCauldronBlock;
import com.livrodassombras.registry.ModBlocks;
import com.livrodassombras.registry.ModEntities;
import com.livrodassombras.registry.ModItems;
import com.livrodassombras.registry.ModParticles;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import com.livrodassombras.ritual.RitualBook;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
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

    /** Ritual cauldron: the vanilla cauldron textures tinted dark purple, and the water texture tinted blood red. */
    private static final int CAULDRON_PURPLE = 0x7B4BA8;
    private static final int CAULDRON_PURPLE_LIT = 0x8E58C2;
    private static final int CAULDRON_BLOOD = 0x8A0A0A;

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> switch (tintIndex) {
            case 0 -> state.hasProperty(RitualCauldronBlock.LIT) && state.getValue(RitualCauldronBlock.LIT)
                    ? CAULDRON_PURPLE_LIT : CAULDRON_PURPLE;
            case 1 -> CAULDRON_BLOOD;
            default -> -1;
        }, ModBlocks.CALDEIRAO_RITUALISTICO.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? CAULDRON_PURPLE : -1,
                ModItems.CALDEIRAO_RITUALISTICO.get());
    }

    /** The Livro do Sangue shows an eye on its cover while a ritual is prepared. */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.LIVRO_DO_SANGUE.get(),
                new ResourceLocation(LivroDasSombras.MOD_ID, "carregado"),
                (stack, level, entity, seed) -> RitualBook.charged(stack) != null ? 1.0F : 0.0F));
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BLOOD_TRAIL.get(), BloodTrailParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.SER_DE_SANGUE, BloodBeingModel::createBodyLayer);
    }
}
