package com.livrodassombras.summon;

import com.livrodassombras.registry.ModItems;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The Livro do Tempo's summons, in menu order. Costs are hunger points (2 = 1 drumstick). */
public enum TimeSummon {
    /** Three Esqueletinhos do Tempo for 4 minutes. One squad at a time. */
    ESQUELETOS("esqueletos", 6, 0, () -> new ItemStack(Items.SKELETON_SKULL)),
    /** The Foice do Tempo. Single use; after it vanishes, 2 minutes before it can be summoned again. */
    FOICE("foice_do_tempo", 3, 20 * 60 * 2, () -> new ItemStack(ModItems.FOICE_DO_TEMPO.get())),
    /** The Relógio Negro, 3 charges. */
    RELOGIO("relogio_negro", 5, 0, () -> new ItemStack(ModItems.RELOGIO_NEGRO.get()));

    private final String id;
    private final int cost;
    private final int cooldownTicks;
    private final Supplier<ItemStack> icon;

    TimeSummon(String id, int cost, int cooldownTicks, Supplier<ItemStack> icon) {
        this.id = id;
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
        this.icon = icon;
    }

    /** Also the id of this summon's personal cooldown. */
    public String id() {
        return this.id;
    }

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
        return this == ESQUELETOS
                ? Component.translatable("gui.livro_das_sombras.livro_do_tempo.esqueletos")
                : this.icon().getHoverName();
    }

    public Component detail() {
        return Component.translatable("gui.livro_das_sombras.livro_do_tempo." + this.id + ".detail");
    }

    @Nullable
    public static TimeSummon byIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : null;
    }
}
