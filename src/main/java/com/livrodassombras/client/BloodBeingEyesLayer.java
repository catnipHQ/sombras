package com.livrodassombras.client;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.entity.BloodBeingEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** The red eyes glow in the dark, like a spider's. */
public class BloodBeingEyesLayer extends EyesLayer<BloodBeingEntity, BloodBeingModel> {
    private static final RenderType EYES = RenderType.eyes(
            new ResourceLocation(LivroDasSombras.MOD_ID, "textures/entity/ser_de_sangue_eyes.png"));

    public BloodBeingEyesLayer(RenderLayerParent<BloodBeingEntity, BloodBeingModel> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }
}
