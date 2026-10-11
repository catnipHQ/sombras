package com.livrodassombras.summon;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Livro dos Espíritos' summons, in menu order. Costs are experience levels.
 * Each has its own cooldown (the player's, like the rituals), on top of the book's usual 5 s.
 */
public enum SpiritSummon {
    /** Two Vex Espirituais for 45 s. */
    VEX("vex", 3, 20 * 30, () -> new ItemStack(Items.VEX_SPAWN_EGG)),
    /** Three Aliados Espirituais for 60 s: weak hits that leave Weakness and Slowness. */
    ALIADOS("aliados", 3, 20 * 30, () -> new ItemStack(Items.SOUL_LANTERN)),
    /** The Golem de Almas, until it dies. Only one. */
    GOLEM("golem", 6, 20 * 90, () -> new ItemStack(Items.CARVED_PUMPKIN));

    private final String id;
    private final int cost;
    private final int cooldownTicks;
    private final Supplier<ItemStack> icon;

    SpiritSummon(String id, int cost, int cooldownTicks, Supplier<ItemStack> icon) {
        this.id = id;
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
        this.icon = icon;
    }

    /** "espiritos_vex", ...: also the id of this summon's personal cooldown. */
    public String id() {
        return "espiritos_" + this.id;
    }

    /** Experience levels. */
    public int cost() {
        return this.cost;
    }

    public int cooldownTicks() {
        return this.cooldownTicks;
    }

    public ItemStack icon() {
        return this.icon.get();
    }

    public Component displayName() {
        return Component.translatable("gui.livro_das_sombras.livro_dos_espiritos." + this.id);
    }

    public Component detail() {
        return Component.translatable("gui.livro_das_sombras.livro_dos_espiritos." + this.id + ".detail");
    }

    @Nullable
    public static SpiritSummon byIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : null;
    }
}
