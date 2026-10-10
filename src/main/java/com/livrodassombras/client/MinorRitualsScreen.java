package com.livrodassombras.client;

import com.livrodassombras.network.BookSectionPacket;
import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.network.SelectRitualPacket;
import com.livrodassombras.ritual.BookPath;
import com.livrodassombras.ritual.ClientRitualCooldowns;
import com.livrodassombras.ritual.MinorRitual;
import com.livrodassombras.ritual.RitualBook;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Rituais menores (locked section) of either book: choosing one charges the book; it's cast later with right-click. */
public class MinorRitualsScreen extends RitualScreen {
    private final BookPath path;
    private boolean anyCooling;

    public MinorRitualsScreen(BookPath path) {
        super(Component.translatable("gui.livro_das_sombras.rituais.title"), 220, BookScreens.theme(path));
        this.path = path;
    }

    @Override
    protected void buildOptions() {
        long now = ClientEvents.gameTime();
        this.anyCooling = false;
        for (MinorRitual ritual : MinorRitual.of(this.path)) {
            float cooldown = ClientRitualCooldowns.fraction(ritual, now);
            this.anyCooling |= cooldown > 0.0F;
            this.options.add(new Option(ritual.displayName(), ritual.detail(), ritual.icon(), null, true, cooldown));
        }
    }

    @Override
    protected Component subtitle() {
        MinorRitual charged = RitualBook.charged(ClientEvents.heldBook());
        return charged == null
                ? Component.translatable("gui.livro_das_sombras.rituais.subtitle")
                : Component.translatable("gui.livro_das_sombras.rituais.preparado", charged.displayName());
    }

    @Override
    protected Component hint() {
        return Component.translatable("gui.livro_das_sombras.ritual.hint_roda", this.options.size(),
                ModKeys.WHEEL.getTranslatedKeyMessage());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.anyCooling && ClientEvents.gameTime() % 2L == 0L) {
            this.rebuild(); // keep the bars moving
        }
    }

    @Override
    protected Component backLabel() {
        return Component.translatable("gui.livro_das_sombras.voltar");
    }

    @Override
    protected void back() {
        ModNetwork.CHANNEL.sendToServer(new BookSectionPacket(RitualBook.SECTION_NONE));
        Minecraft.getInstance().setScreen(new BloodBookRootScreen(this.path));
    }

    @Override
    protected void choose(int index) {
        ModNetwork.CHANNEL.sendToServer(new SelectRitualPacket(index));
        this.onClose();
    }
}
