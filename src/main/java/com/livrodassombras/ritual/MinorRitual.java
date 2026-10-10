package com.livrodassombras.ritual;

import com.livrodassombras.registry.ModItems;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Livro do Sangue's minor rituals (spells). Picking one in the menu "charges" the book;
 * right-click casts it, Shift + right-click cancels it. Costs are in health points (2 = 1 heart).
 */
public enum MinorRitual {
    /** A target up to 8 blocks away: 1 s of warning, then 12 damage over 6 s. */
    DILACERAR("dilacerar", 5.0F, 20 * 20, () -> new ItemStack(Items.FERMENTED_SPIDER_EYE)),
    /** Gives 4 hearts of health to a player or creature up to 8 blocks away. */
    TROCA_DE_SANGUE("troca_de_sangue", 6.0F, 20 * 10, () -> new ItemStack(Items.GLISTERING_MELON_SLICE)),
    /** A blood slash 10 blocks forward: 14 damage to everything it crosses. */
    CORTE_SANGRENTO("corte_sangrento", 8.0F, 20 * 40, () -> new ItemStack(ModItems.ESPADA_DE_SANGUE.get()));

    private final String id;
    private final float cost;
    private final int cooldownTicks;
    private final Supplier<ItemStack> icon;

    MinorRitual(String id, float cost, int cooldownTicks, Supplier<ItemStack> icon) {
        this.id = id;
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
        this.icon = icon;
    }

    public String id() {
        return this.id;
    }

    public float cost() {
        return this.cost;
    }

    public int cooldownTicks() {
        return this.cooldownTicks;
    }

    public ItemStack icon() {
        return this.icon.get();
    }

    public Component displayName() {
        return Component.translatable("ritual.livro_das_sombras." + this.id);
    }

    public Component detail() {
        return Component.translatable("ritual.livro_das_sombras." + this.id + ".detail");
    }

    @Nullable
    public static MinorRitual byId(String id) {
        for (MinorRitual ritual : values()) {
            if (ritual.id.equals(id)) {
                return ritual;
            }
        }
        return null;
    }

    @Nullable
    public static MinorRitual byIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : null;
    }
}
