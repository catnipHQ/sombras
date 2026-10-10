package com.livrodassombras.effect;

import com.livrodassombras.registry.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * Hemorragia (from the Espada de Sangue): a minor bleed.
 * Half a heart of damage every second, blood dripping from the target, and a trail of blood
 * on the ground behind it while it moves (the stains fade after a few seconds).
 */
public class HemorrhageEffect extends MobEffect {
    public static final float DAMAGE = 1.0F;        // half a heart
    public static final int DAMAGE_INTERVAL = 20;   // every second
    private static final int TRAIL_INTERVAL = 3;

    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.65F, 0.0F, 0.0F), 0.9F);

    public HemorrhageEffect() {
        super(MobEffectCategory.HARMFUL, 0x5E0000);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        int tick = entity.tickCount;

        // Drips from the wound.
        if (tick % 4 == 0) {
            double width = entity.getBbWidth() * 0.3D;
            level.sendParticles(BLOOD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                    1, width, entity.getBbHeight() * 0.2D, width, 0.0D);
        }

        // Trail on the ground while it moves (and now and then while it stands still).
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        boolean moving = dx * dx + dz * dz > 0.0004D;
        if (entity.onGround() && tick % TRAIL_INTERVAL == 0 && (moving || tick % 15 == 0)) {
            level.sendParticles(ModParticles.BLOOD_TRAIL.get(), entity.getX(), entity.getY() + 0.02D, entity.getZ(),
                    1, entity.getBbWidth() * 0.2D, 0.0D, entity.getBbWidth() * 0.2D, 0.0D);
        }

        // The damage itself. It must not use up the target's hurt cooldown, or it would swallow the next sword hit.
        if (tick % DAMAGE_INTERVAL == 0) {
            int cooldown = entity.invulnerableTime;
            entity.invulnerableTime = 0;
            entity.hurt(entity.damageSources().magic(), DAMAGE * (amplifier + 1));
            entity.invulnerableTime = Math.min(entity.invulnerableTime, cooldown);
        }
    }
}
