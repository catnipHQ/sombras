package com.livrodassombras.client;

import com.livrodassombras.network.BookSectionPacket;
import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.registry.ModItems;
import com.livrodassombras.ritual.BookPath;
import com.livrodassombras.ritual.RitualBook;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** First page of a ritual book: I. Invocações, II. Rituais menores. Red for the Livro do Sangue, grey for the Livro do Tempo. */
public class BloodBookRootScreen extends RitualScreen {
    private final BookPath path;

    public BloodBookRootScreen(BookPath path) {
        super(Component.translatable("gui.livro_das_sombras." + path.bookId() + ".title"), 200, BookScreens.theme(path));
        this.path = path;
    }

    @Override
    protected void buildOptions() {
        String key = "gui.livro_das_sombras." + this.path.bookId();
        boolean time = this.path == BookPath.TIME;
        this.options.add(new Option(Component.translatable("gui.livro_das_sombras.livro_do_sangue.invocacoes"),
                Component.translatable(key + ".invocacoes.detail"),
                new ItemStack(time ? Items.SKELETON_SKULL : ModItems.ESPADA_DE_SANGUE.get()), null, true));
        this.options.add(new Option(Component.translatable("gui.livro_das_sombras.livro_do_sangue.rituais"),
                Component.translatable(key + ".rituais.detail"),
                new ItemStack(time ? Items.CLOCK : Items.FERMENTED_SPIDER_EYE), null, true));
    }

    @Override
    protected Component subtitle() {
        return Component.translatable("gui.livro_das_sombras.livro_do_sangue.secoes");
    }

    @Override
    protected void choose(int index) {
        // The chosen section stays locked on the book until "Back".
        ModNetwork.CHANNEL.sendToServer(new BookSectionPacket(index));
        Minecraft.getInstance().setScreen(index == RitualBook.SECTION_INVOCACOES
                ? new BloodBookScreen(this.path) : new MinorRitualsScreen(this.path));
    }
}
