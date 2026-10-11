package com.livrodassombras.item;

import com.livrodassombras.ritual.BookPath;
import net.minecraft.ChatFormatting;

/**
 * Livro dos Espíritos: the blue book. Works exactly like the Livro do Sangue and the Livro do Tempo
 * (I. Invocações, II. Rituais menores, locked sections, the R wheel, charged rituals, 5 uses),
 * but its menu is light blue, its magic burns in blue soul fire, and it pays in experience levels.
 * Not obtainable in survival yet: creative tab only.
 */
public class LivroDosEspiritosItem extends LivroDoSangueItem {
    public LivroDosEspiritosItem(Properties properties) {
        super(properties);
    }

    @Override
    public BookPath path() {
        return BookPath.SPIRIT;
    }

    @Override
    protected ChatFormatting usesColor() {
        return ChatFormatting.DARK_AQUA;
    }
}
