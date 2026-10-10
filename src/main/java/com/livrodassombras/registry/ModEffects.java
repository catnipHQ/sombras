package com.livrodassombras.registry;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.effect.BleedingEffect;
import com.livrodassombras.effect.BloodBondEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Status effects. Icons live in assets/livro_das_sombras/textures/mob_effect/. */
public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, LivroDasSombras.MOD_ID);

    public static final RegistryObject<MobEffect> SANGRANDO = EFFECTS.register("sangrando", BleedingEffect::new);
    public static final RegistryObject<MobEffect> SER_DE_SANGUE = EFFECTS.register("ser_de_sangue", BloodBondEffect::new);
}
