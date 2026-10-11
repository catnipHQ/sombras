package com.livrodassombras.item;

import com.livrodassombras.ritual.BookPath;
import net.minecraft.ChatFormatting;

/**
 * Livro do Tempo: the black book with the white clock. Works exactly like the Livro do Sangue
 * (I. Invocações, II. Rituais menores, locked sections, the R wheel, charged rituals, 5 uses),
 * but its menu is black and grey and it pays in hunger instead of blood.
 * Not obtainable in survival yet: creative tab only.
 */
public class LivroDoTempoItem extends LivroDoSangueItem {
    public LivroDoTempoItem(Properties properties) {
        super(properties);
    }

    @Override
    public BookPath path() {
        return BookPath.TIME;
    }

    @Override
    protected ChatFormatting usesColor() {
        return ChatFormatting.GRAY;
    }
}
