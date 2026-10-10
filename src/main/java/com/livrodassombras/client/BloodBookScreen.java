package com.livrodassombras.client;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.network.SelectWeaponPacket;
import com.livrodassombras.summon.BloodWeapons;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Invocações (locked section): the four weapons and the Ser de Sangue. Keys 1-5 choose directly. */
public class BloodBookScreen extends RitualScreen {
    static final ResourceLocation BEING_ICON =
            new ResourceLocation(LivroDasSombras.MOD_ID, "textures/mob_effect/ser_de_sangue.png");

    public BloodBookScreen() {
        super(Component.translatable("gui.livro_das_sombras.livro_do_sangue.title"), 176);
    }

    @Override
    protected void buildOptions() {
        for (Item weapon : BloodWeapons.menuOrder()) {
            this.options.add(new Option(weapon.getDescription(), null, new ItemStack(weapon), null, true));
        }
        // The Ser de Sangue comes last (index BloodWeapons.beingIndex()).
        this.options.add(new Option(Component.translatable("entity.livro_das_sombras.ser_de_sangue"), null,
                ItemStack.EMPTY, BEING_ICON, true));
    }

    @Override
    protected Component subtitle() {
        return Component.translatable("gui.livro_das_sombras.livro_do_sangue.cost");
    }

    @Override
    protected Component hint() {
        return Component.translatable("gui.livro_das_sombras.ritual.hint_roda", this.options.size(),
                ModKeys.WHEEL.getTranslatedKeyMessage());
    }

    @Override
    protected Component backLabel() {
        return Component.translatable("gui.livro_das_sombras.voltar");
    }

    @Override
    protected void back() {
        ModNetwork.CHANNEL.sendToServer(new com.livrodassombras.network.BookSectionPacket(
                com.livrodassombras.ritual.RitualBook.SECTION_NONE));
        net.minecraft.client.Minecraft.getInstance().setScreen(new BloodBookRootScreen());
    }

    @Override
    protected void choose(int index) {
        ModNetwork.CHANNEL.sendToServer(new SelectWeaponPacket(index));
        this.onClose();
    }
}
