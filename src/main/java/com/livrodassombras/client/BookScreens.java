package com.livrodassombras.client;

import com.livrodassombras.ritual.BookPath;

/** Small shared bits of the ritual book menus. */
final class BookScreens {
    private BookScreens() {}

    static RitualScreen.Theme theme(BookPath path) {
        return switch (path) {
            case TIME -> RitualScreen.Theme.TIME;
            case SPIRIT -> RitualScreen.Theme.SPIRIT;
            case BLOOD -> RitualScreen.Theme.BLOOD;
        };
    }
}
