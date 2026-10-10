package com.livrodassombras.registry;

import com.livrodassombras.LivroDasSombras;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, LivroDasSombras.MOD_ID);

    /** A drop of blood lying flat on the ground, fading after a few seconds. */
    public static final RegistryObject<SimpleParticleType> BLOOD_TRAIL =
            PARTICLES.register("blood_trail", () -> new SimpleParticleType(false));
}
