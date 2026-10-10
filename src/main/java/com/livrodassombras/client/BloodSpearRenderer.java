package com.livrodassombras.client;

import com.livrodassombras.entity.BloodSpearEntity;
import com.livrodassombras.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the flying spear as its item sprite, facing the camera and spinning while it moves. */
public class BloodSpearRenderer extends EntityRenderer<BloodSpearEntity> {
    private final ItemRenderer itemRenderer;

    public BloodSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(BloodSpearEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        poseStack.scale(1.8F, 1.8F, 1.8F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        if (entity.getDeltaMovement().lengthSqr() > 0.01D) {
            poseStack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * 45.0F));
        }

        this.itemRenderer.renderStatic(new ItemStack(ModItems.LANCA_DE_SANGUE.get()), ItemDisplayContext.GROUND,
                light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();

        super.render(entity, yaw, partialTick, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BloodSpearEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
