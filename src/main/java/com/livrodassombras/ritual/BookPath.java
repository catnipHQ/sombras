package com.livrodassombras.ritual;

import com.livrodassombras.registry.ModItems;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The paths of ritual books. All work the same way (sections, wheel, charged rituals, uses);
 * the Blood path pays in health, the Time path in hunger, the Spirit path in experience levels.
 */
public enum BookPath {
    BLOOD("livro_do_sangue", ChatFormatting.RED),
    TIME("livro_do_tempo", ChatFormatting.GRAY),
    SPIRIT("livro_dos_espiritos", ChatFormatting.AQUA);

    private final String bookId;
    private final ChatFormatting color;

    BookPath(String bookId, ChatFormatting color) {
        this.bookId = bookId;
        this.color = color;
    }

    /** "livro_do_sangue", "livro_do_tempo" or "livro_dos_espiritos": also the prefix of the book's menu translation keys. */
    public String bookId() {
        return this.bookId;
    }

    public ChatFormatting color() {
        return this.color;
    }

    public Item book() {
        return switch (this) {
            case BLOOD -> ModItems.LIVRO_DO_SANGUE.get();
            case TIME -> ModItems.LIVRO_DO_TEMPO.get();
            case SPIRIT -> ModItems.LIVRO_DOS_ESPIRITOS.get();
        };
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
        if (stack.is(ModItems.LIVRO_DOS_ESPIRITOS.get())) {
            return SPIRIT;
        }
        return null;
    }
}
