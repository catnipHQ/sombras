package com.livrodassombras.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.VexModel;
import net.minecraft.client.model.geom.ModelPart;

/** The vanilla Vex model, drawn in pale soul-fire blue and a little see-through. */
public class SoulVexModel extends VexModel {
    public SoulVexModel(ModelPart root) {
        super(root);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        super.renderToBuffer(poseStack, buffer, light, overlay, red * 0.45F, green * 0.88F, blue, alpha * 0.8F);
    }
}
