package com.livrodassombras.registry;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.entity.BloodBeingEntity;
import com.livrodassombras.entity.BloodSpearEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LivroDasSombras.MOD_ID);

    public static final RegistryObject<EntityType<BloodSpearEntity>> LANCA_DE_SANGUE = ENTITIES.register(
            "lanca_de_sangue",
            () -> EntityType.Builder.<BloodSpearEntity>of(BloodSpearEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("lanca_de_sangue"));

    public static final RegistryObject<EntityType<BloodBeingEntity>> SER_DE_SANGUE = ENTITIES.register(
            "ser_de_sangue",
            () -> EntityType.Builder.<BloodBeingEntity>of(BloodBeingEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("ser_de_sangue"));
}
