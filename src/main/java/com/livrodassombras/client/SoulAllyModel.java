package com.livrodassombras.client;

import com.livrodassombras.entity.SoulAllyEntity;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/** A hooded ghost: the humanoid body with arms reaching forward like a zombie's. */
public class SoulAllyModel extends HumanoidModel<SoulAllyEntity> {
    public SoulAllyModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(SoulAllyEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, true, this.attackTime, ageInTicks);
    }
}
