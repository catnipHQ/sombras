package com.livrodassombras.effect;

import com.livrodassombras.ritual.TimeRituals;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

/**
 * Parado no Tempo (from the Relógio Negro): the mob's mind stops. While it lasts the mob has no AI
 * (it can't walk, look around or attack) and only falls. Players and bosses never get this effect;
 * they get Slowness III instead (see the Relógio Negro).
 * <p>
 * The AI is switched off when the effect starts and back on when it ends, but only if this effect
 * was what switched it off (a mob that already had NoAI keeps it).
 */
public class ParalysisEffect extends MobEffect {
    private static final String FROZE_TAG = "veritates_parado";

    public ParalysisEffect() {
        super(MobEffectCategory.HARMFUL, 0x1C1C1E);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        mob.getNavigation().stop();
        Vec3 motion = mob.getDeltaMovement();
        mob.setDeltaMovement(0.0D, Math.min(motion.y, 0.0D), 0.0D);
        if (mob instanceof Creeper creeper) {
            creeper.setSwellDir(-1); // a creeper about to blow stops swelling
        }
        if (mob.tickCount % 5 == 0 && mob.level() instanceof ServerLevel level) {
            level.sendParticles(TimeRituals.GREY, mob.getX(), mob.getY() + mob.getBbHeight() + 0.25D, mob.getZ(),
                    1, mob.getBbWidth() * 0.3D, 0.05D, mob.getBbWidth() * 0.3D, 0.0D);
        }
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(entity, attributes, amplifier);
        if (entity instanceof Mob mob && !mob.isNoAi()) {
            mob.setNoAi(true);
            mob.getPersistentData().putBoolean(FROZE_TAG, true);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        if (entity instanceof Mob mob && mob.getPersistentData().getBoolean(FROZE_TAG)) {
            mob.setNoAi(false);
            mob.getPersistentData().remove(FROZE_TAG);
        }
    }
}
