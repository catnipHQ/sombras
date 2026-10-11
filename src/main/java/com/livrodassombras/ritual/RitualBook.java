package com.livrodassombras.ritual;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * What a Livro do Sangue stack remembers: the section it's locked on and the ritual it's charged with.
 * Kept on the stack so the client sees it too. (Ritual cooldowns belong to the player: {@link RitualCooldowns}.)
 */
public final class RitualBook {
    public static final int SECTION_NONE = -1;
    public static final int SECTION_INVOCACOES = 0;
    public static final int SECTION_RITUAIS = 1;

    private static final String CHARGED = "Ritual";
    private static final String SECTION = "Section";

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

    public static int section(ItemStack book) {
        CompoundTag tag = book.getTag();
        return tag != null && tag.contains(SECTION) ? tag.getInt(SECTION) : SECTION_NONE;
    }

    /** Locking on another section (or unlocking) also drops a prepared ritual. */
    public static void setSection(ItemStack book, int section) {
        if (section != SECTION_RITUAIS) {
            discharge(book);
        }
        if (section == SECTION_NONE) {
            CompoundTag tag = book.getTag();
            if (tag != null) {
                tag.remove(SECTION);
            }
        } else {
            book.getOrCreateTag().putInt(SECTION, section);
        }
    }
}
