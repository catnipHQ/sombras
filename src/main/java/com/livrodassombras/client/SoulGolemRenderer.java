package com.livrodassombras.client;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.entity.SoulGolemEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * The Golem de Almas: the Iron Golem's shape in see-through soul glass, with blue flames rising from its legs.
 * Larger and darker in the Destroyer form.
 */
public class SoulGolemRenderer extends MobRenderer<SoulGolemEntity, SoulGolemModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(LivroDasSombras.MOD_ID, "textures/entity/golem_de_almas.png");
    private static final ResourceLocation DESTROYER =
            new ResourceLocation(LivroDasSombras.MOD_ID, "textures/entity/golem_de_almas_destruidor.png");

    public SoulGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new SoulGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(SoulGolemEntity entity) {
        return entity.getForm() == SoulGolemEntity.Form.DESTROYER ? DESTROYER : TEXTURE;
    }

    @Nullable
    @Override
    protected RenderType getRenderType(SoulGolemEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return bodyVisible ? RenderType.entityTranslucent(this.getTextureLocation(entity))
                : super.getRenderType(entity, bodyVisible, translucent, glowing);
    }

    @Override
    protected void scale(SoulGolemEntity entity, PoseStack poseStack, float partialTick) {
        if (entity.getForm() == SoulGolemEntity.Form.DESTROYER) {
            float s = SoulGolemEntity.DESTROYER_SCALE;
            poseStack.scale(s, s, s);
        }
    }

    /** The Iron Golem's side-to-side sway while walking. */
    @Override
    protected void setupRotations(SoulGolemEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw,
                                  float partialTick) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);
        if (entity.walkAnimation.speed() >= 0.01F) {
            float phase = entity.walkAnimation.position(partialTick) + 6.0F;
            float sway = (Math.abs(phase % 13.0F - 6.5F) - 3.25F) / 3.25F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * sway));
        }
    }

    @Override
    protected int getBlockLightLevel(SoulGolemEntity entity, BlockPos pos) {
        return Math.max(9, super.getBlockLightLevel(entity, pos));
    }
}
