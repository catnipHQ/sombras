package com.livrodassombras.event;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps the Espada de Sangue bound to its owner: it can never be dropped. */
@Mod.EventBusSubscriber(modid = LivroDasSombras.MOD_ID)
public class SwordBindingEvents {

    /** Q key, dragging out of the inventory, etc. The drop is cancelled and the sword goes back. */
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!stack.is(ModItems.ESPADA_DE_SANGUE.get())) {
            return;
        }

        event.setCanceled(true);

        Player player = event.getPlayer();
        // On death the sword simply disappears instead of piling up as loot.
        if (player.level().isClientSide || !player.isAlive()) {
            return;
        }

        // Put it back in the hand slot it came from when possible.
        Inventory inventory = player.getInventory();
        if (inventory.getSelected().isEmpty()) {
            inventory.setItem(inventory.selected, stack);
        } else {
            inventory.add(stack);
        }
        player.displayClientMessage(Component.translatable("message.livro_das_sombras.nao_pode_largar"), true);
    }

    /**
     * When the player dies the sword disappears, even with keepInventory on.
     * (A Totem of Undying prevents the death, so the sword stays in that case.)
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(ModItems.ESPADA_DE_SANGUE.get())) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        if (player.containerMenu.getCarried().is(ModItems.ESPADA_DE_SANGUE.get())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    /** Safety net: the sword never shows up in death drops. */
    @SubscribeEvent
    public static void onDeathDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(drop -> drop.getItem().is(ModItems.ESPADA_DE_SANGUE.get()));
    }
}
