package com.livrodassombras.summon;

import com.livrodassombras.LivroDasSombras;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Creatures made of bone have no blood: skeletons (any kind), the Wither, skeleton horses and the
 * Esqueletinhos do Tempo. Zombies and zombified piglins still have flesh, so they still bleed.
 * The list is the entity type tag {@code livro_das_sombras:sem_sangue}, so a datapack can add other mods' creatures.
 */
public final class Bloodless {
    public static final TagKey<EntityType<?>> TAG =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(LivroDasSombras.MOD_ID, "sem_sangue"));

    private Bloodless() {}

    public static boolean is(Entity entity) {
        return entity.getType().is(TAG);
    }
}
