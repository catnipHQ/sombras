package com.livrodassombras.summon;

import com.livrodassombras.registry.ModItems;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The list of summonable weapons. Add new weapons here and they get the same rules automatically. */
public final class BloodWeapons {
    private BloodWeapons() {}

    /** Order matches the buttons of the Livro do Sangue menu. */
    public static List<Item> menuOrder() {
        return List.of(
                ModItems.ESPADA_DE_SANGUE.get(),
                ModItems.LANCA_DE_SANGUE.get(),
                ModItems.MARTELO_DE_SANGUE.get(),
                ModItems.ARCO_DE_SANGUE.get());
    }

    /** Menu index of the Ser de Sangue, which comes right after the weapons. */
    public static int beingIndex() {
        return menuOrder().size();
    }

    /** @return the weapon for a menu index, or null if the index is invalid. */
    public static Item byIndex(int index) {
        List<Item> weapons = menuOrder();
        return index >= 0 && index < weapons.size() ? weapons.get(index) : null;
    }

    /**
     * True for any summoned item, from either book (the Livro do Tempo's Foice and Relógio Negro too):
     * they all follow the same rules (one at a time, vanish when dropped, stored or on death).
     */
    public static boolean is(ItemStack stack) {
        return !stack.isEmpty() && (menuOrder().contains(stack.getItem())
                || stack.is(ModItems.FOICE_DO_TEMPO.get()) || stack.is(ModItems.RELOGIO_NEGRO.get()));
    }

    /** True if the player carries any summoned weapon (hotbar, inventory, armor or offhand). */
    public static boolean hasAny(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (is(inventory.getItem(i))) {
                return true;
            }
        }
        return false;
    }
}
