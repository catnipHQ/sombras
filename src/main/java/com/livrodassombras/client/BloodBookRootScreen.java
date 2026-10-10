package com.livrodassombras.client;

import com.livrodassombras.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** First page of the Livro do Sangue: I. Invocações (weapons and Ser de Sangue), II. Rituais menores. */
public class BloodBookRootScreen extends RitualScreen {
    public BloodBookRootScreen() {
        super(Component.translatable("gui.livro_das_sombras.livro_do_sangue.title"), 200);
    }

    @Override
    protected void buildOptions() {
        this.options.add(new Option(Component.translatable("gui.livro_das_sombras.livro_do_sangue.invocacoes"),
                Component.translatable("gui.livro_das_sombras.livro_do_sangue.invocacoes.detail"),
                new ItemStack(ModItems.ESPADA_DE_SANGUE.get()), null, true));
        this.options.add(new Option(Component.translatable("gui.livro_das_sombras.livro_do_sangue.rituais"),
                Component.translatable("gui.livro_das_sombras.livro_do_sangue.rituais.detail"),
                new ItemStack(Items.FERMENTED_SPIDER_EYE), null, true));
    }

    @Override
    protected Component subtitle() {
        return Component.translatable("gui.livro_das_sombras.livro_do_sangue.secoes");
    }

    @Override
    protected void choose(int index) {
        Minecraft.getInstance().setScreen(index == 0 ? new BloodBookScreen() : new MinorRitualsScreen());
    }
}
