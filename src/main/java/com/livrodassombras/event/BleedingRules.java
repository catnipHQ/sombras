package com.livrodassombras.event;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.registry.ModEffects;
import com.livrodassombras.summon.Bloodless;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Bone creatures don't bleed: Hemorragia (Espada de Sangue) and Sangrando (thrown Lança de Sangue)
 * never take hold on them, whatever applies them (weapons, commands, other mods).
 */
@Mod.EventBusSubscriber(modid = LivroDasSombras.MOD_ID)
public final class BleedingRules {
    private BleedingRules() {}

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        MobEffect effect = event.getEffectInstance().getEffect();
        if ((effect == ModEffects.HEMORRAGIA.get() || effect == ModEffects.SANGRANDO.get())
                && Bloodless.is(event.getEntity())) {
            event.setResult(Event.Result.DENY);
        }
    }
}
