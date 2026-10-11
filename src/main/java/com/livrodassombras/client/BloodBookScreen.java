package com.livrodassombras.client;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.network.BookSectionPacket;
import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.network.SelectWeaponPacket;
import com.livrodassombras.ritual.BookPath;
import com.livrodassombras.ritual.ClientRitualCooldowns;
import com.livrodassombras.ritual.RitualBook;
import com.livrodassombras.summon.BloodWeapons;
import com.livrodassombras.summon.SpiritSummon;
import com.livrodassombras.summon.TimeSummon;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Invocações (locked section). Livro do Sangue: the four weapons and the Ser de Sangue.
 * Livro do Tempo: the Esqueletinhos, the Foice do Tempo and the Relógio Negro.
 * Livro dos Espíritos: the Vex, the Aliados and the Golem de Almas. Number keys choose directly.
 */
public class BloodBookScreen extends RitualScreen {
    static final ResourceLocation BEING_ICON =
            new ResourceLocation(LivroDasSombras.MOD_ID, "textures/mob_effect/ser_de_sangue.png");

    private final BookPath path;
    private boolean anyCooling;

    public BloodBookScreen(BookPath path) {
        super(Component.translatable("gui.livro_das_sombras." + path.bookId() + ".title"), 176, BookScreens.theme(path));
        this.path = path;
    }

    @Override
    protected void buildOptions() {
        if (this.path == BookPath.SPIRIT) {
            long now = ClientEvents.gameTime();
            this.anyCooling = false;
            for (SpiritSummon summon : SpiritSummon.values()) {
                float cooldown = ClientRitualCooldowns.fraction(summon.id(), summon.cooldownTicks(), now);
                this.anyCooling |= cooldown > 0.0F;
                this.options.add(new Option(summon.displayName(), summon.detail(), summon.icon(), null, true, cooldown));
            }
            return;
        }
        if (this.path == BookPath.TIME) {
            long now = ClientEvents.gameTime();
            this.anyCooling = false;
            for (TimeSummon summon : TimeSummon.values()) {
                float cooldown = ClientRitualCooldowns.fraction(summon.id(), summon.cooldownTicks(), now);
                this.anyCooling |= cooldown > 0.0F;
                this.options.add(new Option(summon.displayName(), summon.detail(), summon.icon(), null, true, cooldown));
            }
            return;
        }
        for (Item weapon : BloodWeapons.menuOrder()) {
            this.options.add(new Option(weapon.getDescription(), null, new ItemStack(weapon), null, true));
        }
        // The Ser de Sangue comes last (index BloodWeapons.beingIndex()).
        this.options.add(new Option(Component.translatable("entity.livro_das_sombras.ser_de_sangue"), null,
                ItemStack.EMPTY, BEING_ICON, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.anyCooling && ClientEvents.gameTime() % 2L == 0L) {
            this.rebuild(); // keep the bars moving
        }
    }

    @Override
    protected Component subtitle() {
        return Component.translatable("gui.livro_das_sombras." + this.path.bookId() + ".cost");
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
        ModNetwork.CHANNEL.sendToServer(new BookSectionPacket(RitualBook.SECTION_NONE));
        Minecraft.getInstance().setScreen(new BloodBookRootScreen(this.path));
    }

    @Override
    protected void choose(int index) {
        ModNetwork.CHANNEL.sendToServer(new SelectWeaponPacket(index));
        this.onClose();
    }
}
