package com.livrodassombras.client;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.entity.SoulAllyEntity;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** The Aliado Espiritual: a see-through hooded ghost whose legs fade into blue flame. */
public class SoulAllyRenderer extends HumanoidMobRenderer<SoulAllyEntity, SoulAllyModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(LivroDasSombras.MOD_ID, "textures/entity/aliado_espiritual.png");

    public SoulAllyRenderer(EntityRendererProvider.Context context) {
        super(context, new SoulAllyModel(context.bakeLayer(ModelLayers.ZOMBIE)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(SoulAllyEntity entity) {
        return TEXTURE;
    }

    @Nullable
    @Override
    protected RenderType getRenderType(SoulAllyEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return bodyVisible ? RenderType.entityTranslucent(TEXTURE)
                : super.getRenderType(entity, bodyVisible, translucent, glowing);
    }

    @Override
    protected int getBlockLightLevel(SoulAllyEntity entity, BlockPos pos) {
        return Math.max(10, super.getBlockLightLevel(entity, pos));
    }
}
