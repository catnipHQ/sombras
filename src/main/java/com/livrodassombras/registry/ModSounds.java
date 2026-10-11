package com.livrodassombras.registry;

import com.livrodassombras.LivroDasSombras;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Sound events. The actual audio is defined in assets/livro_das_sombras/sounds.json. */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, LivroDasSombras.MOD_ID);

    public static final RegistryObject<SoundEvent> BLOOD_SUMMON = register("blood_summon");
    public static final RegistryObject<SoundEvent> BLOOD_SQUELCH = register("blood_squelch");
    public static final RegistryObject<SoundEvent> TIME_SUMMON = register("time_summon");
    public static final RegistryObject<SoundEvent> SOUL_SUMMON = register("soul_summon");
    public static final RegistryObject<SoundEvent> SOUL_WHISPER = register("soul_whisper");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () ->
                SoundEvent.createVariableRangeEvent(new ResourceLocation(LivroDasSombras.MOD_ID, name)));
    }
}
