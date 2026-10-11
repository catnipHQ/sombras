package com.livrodassombras.client;

import com.livrodassombras.entity.SoulGolemEntity;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** The Iron Golem's model. While charging a Colossus slam, both arms rise over the head and tremble. */
public class SoulGolemModel extends IronGolemModel<SoulGolemEntity> {
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public SoulGolemModel(ModelPart root) {
        super(root);
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    @Override
    public void setupAnim(SoulGolemEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float progress = entity.chargeProgress(ageInTicks - entity.tickCount);
        if (progress > 0.0F) {
            float shake = Mth.sin(ageInTicks * 2.2F) * 0.08F * progress;
            this.rightArm.xRot = -2.8F * progress + shake;
            this.leftArm.xRot = -2.8F * progress - shake;
            this.rightArm.zRot = 0.3F * progress;
            this.leftArm.zRot = -0.3F * progress;
        } else {
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
        }
    }
}
