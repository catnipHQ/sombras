package com.livrodassombras.client;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.entity.BloodBeingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BloodBeingRenderer extends HumanoidMobRenderer<BloodBeingEntity, BloodBeingModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(LivroDasSombras.MOD_ID, "textures/entity/ser_de_sangue.png");

    public BloodBeingRenderer(EntityRendererProvider.Context context) {
        super(context, new BloodBeingModel(context.bakeLayer(ModModelLayers.SER_DE_SANGUE)), 0.5F);
        this.addLayer(new BloodBeingEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(BloodBeingEntity entity) {
        return TEXTURE;
    }
}
