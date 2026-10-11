package com.livrodassombras.client;

import net.minecraft.client.model.VexModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Vex;

/**
 * The Vex Espiritual: the vanilla Vex model and textures, tinted blue (see {@link SoulVexModel}), always fully lit.
 * Typed on Vex because the vanilla model is; registered for the Vex Espiritual in {@link ClientSetup}.
 */
public class SoulVexRenderer extends MobRenderer<Vex, VexModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/illager/vex.png");
    private static final ResourceLocation CHARGING = new ResourceLocation("textures/entity/illager/vex_charging.png");

    public SoulVexRenderer(EntityRendererProvider.Context context) {
        super(context, new SoulVexModel(context.bakeLayer(ModelLayers.VEX)), 0.3F);
    }

    @Override
    protected int getBlockLightLevel(Vex entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(Vex entity) {
        return entity.isCharging() ? CHARGING : TEXTURE;
    }
}
