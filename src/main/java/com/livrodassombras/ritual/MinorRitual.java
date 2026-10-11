package com.livrodassombras.ritual;

import com.livrodassombras.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The ritual books' minor rituals (spells). Picking one in the menu "charges" the book;
 * right-click casts it, Shift + right-click cancels it.
 * Costs: on the Blood path in health points (2 = 1 heart), on the Time path in hunger points (2 = 1 drumstick),
 * on the Spirit path in experience levels.
 */
public enum MinorRitual {
    // ---- Livro do Sangue
    /** A target up to 8 blocks away: 1 s of warning, then 12 damage over 6 s. Not on bone creatures. */
    DILACERAR("dilacerar", BookPath.BLOOD, 5.0F, 20 * 20, () -> new ItemStack(Items.FERMENTED_SPIDER_EYE)),
    /** Gives 4 hearts of health to a player or creature up to 8 blocks away. */
    TROCA_DE_SANGUE("troca_de_sangue", BookPath.BLOOD, 6.0F, 20 * 10, () -> new ItemStack(Items.GLISTERING_MELON_SLICE)),
    /** A blood slash 10 blocks forward: 14 damage to everything it crosses. */
    CORTE_SANGRENTO("corte_sangrento", BookPath.BLOOD, 8.0F, 20 * 40, () -> new ItemStack(ModItems.ESPADA_DE_SANGUE.get())),

    // ---- Livro do Tempo
    /** 1 s to cast, then Slowness II for 8 s on the target and whatever stands right next to it. */
    LENTIDAO("lentidao", BookPath.TIME, 2.0F, 20 * 15, () -> new ItemStack(Items.COBWEB)),
    /** 1.5 s to cast, then Wither II and Weakness for 6 s on the target. */
    MURCHAR("murchar", BookPath.TIME, 4.0F, 20 * 25, () -> new ItemStack(Items.WITHER_ROSE)),
    /** First cast marks the spot, the next one brings you back to it (if you didn't go too far). */
    REVERTER_PASSOS("reverter_passos", BookPath.TIME, 3.0F, 20 * 30, () -> new ItemStack(Items.RECOVERY_COMPASS)),

    // ---- Livro dos Espíritos (costs in experience levels)
    /** Two linked soul portals: one in front of you, one where you look (up to 32 blocks). Open for 12 s. */
    PORTAL("portal", BookPath.SPIRIT, 2.0F, 20 * 25, () -> new ItemStack(Items.ENDER_EYE)),
    /** A cone of blue flames 7 blocks forward: 6 damage (ignores armor) and 3 s of fire. */
    FOGO_DAS_ALMAS("fogo_das_almas", BookPath.SPIRIT, 2.0F, 20 * 12, () -> new ItemStack(Items.SOUL_TORCH)),
    /** Swap places with the creature or player you look at, up to 24 blocks away. */
    TROCA("troca", BookPath.SPIRIT, 1.0F, 20 * 8, () -> new ItemStack(Items.CHORUS_FRUIT));

    private final String id;
    private final BookPath path;
    private final float cost;
    private final int cooldownTicks;
    private final Supplier<ItemStack> icon;

    MinorRitual(String id, BookPath path, float cost, int cooldownTicks, Supplier<ItemStack> icon) {
        this.id = id;
        this.path = path;
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
        this.icon = icon;
    }

    public String id() {
        return this.id;
    }

    public BookPath path() {
        return this.path;
    }

    /** Health points (Blood), hunger points (Time) or experience levels (Spirit). */
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

    /** The rituals of one book, in menu order. */
    public static List<MinorRitual> of(BookPath path) {
        List<MinorRitual> list = new ArrayList<>();
        for (MinorRitual ritual : values()) {
            if (ritual.path == path) {
                list.add(ritual);
            }
        }
        return list;
    }

    /** A menu index within one book's rituals. */
    @Nullable
    public static MinorRitual byIndex(BookPath path, int index) {
        List<MinorRitual> list = of(path);
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }
}
