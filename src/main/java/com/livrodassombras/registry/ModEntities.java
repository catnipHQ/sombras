package com.livrodassombras.registry;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.entity.BloodBeingEntity;
import com.livrodassombras.entity.BloodSpearEntity;
import com.livrodassombras.entity.SoulAllyEntity;
import com.livrodassombras.entity.SoulGolemEntity;
import com.livrodassombras.entity.SoulPortalEntity;
import com.livrodassombras.entity.SoulVexEntity;
import com.livrodassombras.entity.TimeSkeletonEntity;
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

    /** Esqueletinho do Tempo: a baby skeleton (the size is halved by isBaby()). */
    public static final RegistryObject<EntityType<TimeSkeletonEntity>> ESQUELETO_DO_TEMPO = ENTITIES.register(
            "esqueleto_do_tempo",
            () -> EntityType.Builder.<TimeSkeletonEntity>of(TimeSkeletonEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.99F)
                    .clientTrackingRange(8)
                    .build("esqueleto_do_tempo"));

    // Livro dos Espíritos
    public static final RegistryObject<EntityType<SoulVexEntity>> VEX_ESPIRITUAL = ENTITIES.register(
            "vex_espiritual",
            () -> EntityType.Builder.<SoulVexEntity>of(SoulVexEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.8F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build("vex_espiritual"));

    public static final RegistryObject<EntityType<SoulAllyEntity>> ALIADO_ESPIRITUAL = ENTITIES.register(
            "aliado_espiritual",
            () -> EntityType.Builder.<SoulAllyEntity>of(SoulAllyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build("aliado_espiritual"));

    public static final RegistryObject<EntityType<SoulGolemEntity>> GOLEM_DE_ALMAS = ENTITIES.register(
            "golem_de_almas",
            () -> EntityType.Builder.<SoulGolemEntity>of(SoulGolemEntity::new, MobCategory.MISC)
                    .sized(1.4F, 2.7F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("golem_de_almas"));

    /** Short-lived, never saved: a pair lasts 12 seconds. */
    public static final RegistryObject<EntityType<SoulPortalEntity>> PORTAL_DE_ALMAS = ENTITIES.register(
            "portal_de_almas",
            () -> EntityType.Builder.<SoulPortalEntity>of(SoulPortalEntity::new, MobCategory.MISC)
                    .sized(1.6F, 2.0F)
                    .fireImmune()
                    .noSave()
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("portal_de_almas"));
}
