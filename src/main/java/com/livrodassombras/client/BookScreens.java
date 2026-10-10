package com.livrodassombras.client;

import com.livrodassombras.ritual.BookPath;

/** Small shared bits of the ritual book menus. */
final class BookScreens {
    private BookScreens() {}

    static RitualScreen.Theme theme(BookPath path) {
        return path == BookPath.TIME ? RitualScreen.Theme.TIME : RitualScreen.Theme.BLOOD;
    }
}
