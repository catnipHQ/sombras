package com.livrodassombras.client;

import com.livrodassombras.entity.TimeSkeletonEntity;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The Esqueletinho do Tempo: the vanilla skeleton model and texture, drawn as a baby (big head, half size). */
public class TimeSkeletonRenderer extends HumanoidMobRenderer<TimeSkeletonEntity, SkeletonModel<TimeSkeletonEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/skeleton/skeleton.png");

    public TimeSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(TimeSkeletonEntity entity) {
        return TEXTURE;
    }
}
