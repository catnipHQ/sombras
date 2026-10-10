package com.livrodassombras.ritual;

import com.livrodassombras.registry.ModItems;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The two paths of ritual books. Both work the same way (sections, wheel, charged rituals, uses);
 * the Blood path pays in health, the Time path pays in hunger.
 */
public enum BookPath {
    BLOOD("livro_do_sangue", ChatFormatting.RED),
    TIME("livro_do_tempo", ChatFormatting.GRAY);

    private final String bookId;
    private final ChatFormatting color;

    BookPath(String bookId, ChatFormatting color) {
        this.bookId = bookId;
        this.color = color;
    }

    /** "livro_do_sangue" or "livro_do_tempo": also the prefix of the book's menu translation keys. */
    public String bookId() {
        return this.bookId;
    }

    public ChatFormatting color() {
        return this.color;
    }

    public Item book() {
        return this == BLOOD ? ModItems.LIVRO_DO_SANGUE.get() : ModItems.LIVRO_DO_TEMPO.get();
    }

    /** The path of a ritual book stack, or null if it isn't one. */
    @Nullable
    public static BookPath of(ItemStack stack) {
        if (stack.is(ModItems.LIVRO_DO_SANGUE.get())) {
            return BLOOD;
        }
        if (stack.is(ModItems.LIVRO_DO_TEMPO.get())) {
            return TIME;
        }
        return null;
    }
}
