package com.livrodassombras.summon;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

/**
 * Who is on whose side. A player's side is the player, their tamed animals and every creature
 * they summoned with any book (Ser de Sangue, Esqueletinhos, Vex, Aliados, Golem de Almas).
 * Rituals, area attacks and summons all use this so nobody hurts their own side.
 */
public final class Allegiance {
    private Allegiance() {}

    /** True if {@code entity} is a pet or summon that belongs to {@code owner}. */
    public static boolean belongsTo(@Nullable Entity entity, @Nullable Entity owner) {
        if (entity == null || owner == null) {
            return false;
        }
        if (entity instanceof TamableAnimal pet && owner instanceof LivingEntity livingOwner) {
            return pet.isOwnedBy(livingOwner);
        }
        if (entity instanceof SpiritBound spirit) {
            UUID id = spirit.getSummonerUUID();
            return id != null && id.equals(owner.getUUID());
        }
        return false;
    }

    /** True if {@code entity} is {@code owner} or on {@code owner}'s side. */
    public static boolean sameSide(@Nullable Entity entity, @Nullable Entity owner) {
        return entity != null && (entity == owner || belongsTo(entity, owner));
    }
}
