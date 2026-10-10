package com.livrodassombras.ritual;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * What a Livro do Sangue stack remembers about rituals: the one it's charged with and when each
 * ritual is ready again (in game time). Kept on the stack so the client sees it too.
 */
public final class RitualBook {
    private static final String CHARGED = "Ritual";
    private static final String COOLDOWNS = "RitualCooldowns";

    private RitualBook() {}

    @Nullable
    public static MinorRitual charged(ItemStack book) {
        CompoundTag tag = book.getTag();
        return tag != null && tag.contains(CHARGED) ? MinorRitual.byId(tag.getString(CHARGED)) : null;
    }

    public static void charge(ItemStack book, MinorRitual ritual) {
        book.getOrCreateTag().putString(CHARGED, ritual.id());
    }

    public static void discharge(ItemStack book) {
        CompoundTag tag = book.getTag();
        if (tag != null) {
            tag.remove(CHARGED);
        }
    }

    /** Ticks left before {@code ritual} can be cast again with this book (0 when ready). */
    public static long cooldownLeft(ItemStack book, MinorRitual ritual, long gameTime) {
        CompoundTag tag = book.getTag();
        if (tag == null || !tag.contains(COOLDOWNS)) {
            return 0L;
        }
        return Math.max(0L, tag.getCompound(COOLDOWNS).getLong(ritual.id()) - gameTime);
    }

    public static void startCooldown(ItemStack book, MinorRitual ritual, long gameTime) {
        CompoundTag tag = book.getOrCreateTag();
        CompoundTag cooldowns = tag.getCompound(COOLDOWNS);
        cooldowns.putLong(ritual.id(), gameTime + ritual.cooldownTicks());
        tag.put(COOLDOWNS, cooldowns);
    }

    /** Whole seconds, rounded up, for messages. */
    public static long seconds(long ticks) {
        return (ticks + 19L) / 20L;
    }
}
