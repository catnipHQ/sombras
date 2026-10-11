package com.livrodassombras.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/** Sangrando: purely visual, the target drips blood while it lasts (applied by the thrown Lança de Sangue). */
public class BleedingEffect extends MobEffect {
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.0F), 1.1F);
    private static final DustParticleOptions DARK_BLOOD = new DustParticleOptions(new Vector3f(0.38F, 0.0F, 0.0F), 0.9F);

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A0303);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 2 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        double width = entity.getBbWidth() * 0.35D;
        double height = entity.getBbHeight();
        level.sendParticles(BLOOD, entity.getX(), entity.getY() + height * 0.6D, entity.getZ(),
                2, width, height * 0.25D, width, 0.0D);
        level.sendParticles(DARK_BLOOD, entity.getX(), entity.getY() + height * 0.3D, entity.getZ(),
                1, width, height * 0.2D, width, 0.0D);
    }
}
