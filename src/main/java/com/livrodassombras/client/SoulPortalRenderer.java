package com.livrodassombras.client;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.entity.SoulPortalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * The soul portal: an upright, glowing round disc as tall as a player, made of two swirls
 * turning opposite ways. It opens and closes by growing from (and shrinking to) its centre.
 */
public class SoulPortalRenderer extends EntityRenderer<SoulPortalEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(LivroDasSombras.MOD_ID, "textures/entity/portal_de_almas.png");

    public SoulPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SoulPortalEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light) {
        float open = entity.openness(partialTick);
        if (open > 0.01F) {
            float age = entity.tickCount + partialTick;
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
            poseStack.pushPose();
            poseStack.translate(0.0D, 1.0D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
            poseStack.scale(open, open, open);

            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * 3.0F));
            disc(poseStack, vc, 1.0F, 0.0F, 235);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(-age * 5.0F));
            disc(poseStack, vc, 0.78F, 0.01F, 150);
            poseStack.popPose();

            poseStack.popPose();
        }
        super.render(entity, yaw, partialTick, poseStack, buffer, light);
    }

    private static void disc(PoseStack poseStack, VertexConsumer vc, float r, float z, int alpha) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        vertex(vc, m, n, -r, -r, z, 0.0F, 1.0F, alpha);
        vertex(vc, m, n, r, -r, z, 1.0F, 1.0F, alpha);
        vertex(vc, m, n, r, r, z, 1.0F, 0.0F, alpha);
        vertex(vc, m, n, -r, r, z, 0.0F, 0.0F, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, int alpha) {
        vc.vertex(m, x, y, z)
                .color(255, 255, 255, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(n, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SoulPortalEntity entity) {
        return TEXTURE;
    }
}
