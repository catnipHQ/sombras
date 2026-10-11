package com.livrodassombras.registry;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.effect.BleedingEffect;
import com.livrodassombras.effect.BloodBondEffect;
import com.livrodassombras.effect.HemorrhageEffect;
import com.livrodassombras.effect.ParalysisEffect;
import com.livrodassombras.effect.TimerEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Status effects. Icons live in assets/livro_das_sombras/textures/mob_effect/. */
public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, LivroDasSombras.MOD_ID);

    public static final RegistryObject<MobEffect> SANGRANDO = EFFECTS.register("sangrando", BleedingEffect::new);
    public static final RegistryObject<MobEffect> HEMORRAGIA = EFFECTS.register("hemorragia", HemorrhageEffect::new);
    public static final RegistryObject<MobEffect> SER_DE_SANGUE = EFFECTS.register("ser_de_sangue", BloodBondEffect::new);

    // Livro do Tempo
    public static final RegistryObject<MobEffect> PARALISIA = EFFECTS.register("paralisia", ParalysisEffect::new);
    public static final RegistryObject<MobEffect> ESQUELETOS_DO_TEMPO =
            EFFECTS.register("esqueletos_do_tempo", () -> new TimerEffect(0xD6D6D2));

    // Livro dos Espíritos
    public static final RegistryObject<MobEffect> VEX_ESPIRITUAIS =
            EFFECTS.register("vex_espirituais", () -> new TimerEffect(0x6FE0FF));
    public static final RegistryObject<MobEffect> ALIADOS_ESPIRITUAIS =
            EFFECTS.register("aliados_espirituais", () -> new TimerEffect(0x9FEAF5));
}
