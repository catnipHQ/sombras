package com.livrodassombras.event;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.registry.ModItems;
import com.livrodassombras.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * The Espada de Sangue dissipates when it leaves its owner:
 * thrown on the ground, stored in a chest-like container, or when the owner dies.
 */
@Mod.EventBusSubscriber(modid = LivroDasSombras.MOD_ID)
public class SwordBindingEvents {

    /** Any sword item entity (Q key, inventory drop, dispenser...) turns into blood particles. */
    @SubscribeEvent
    public static void onItemEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }
        if (!itemEntity.getItem().is(ModItems.ESPADA_DE_SANGUE.get())) {
            return;
        }
        if (event.getLevel().isClientSide) {
            return;
        }

        event.setCanceled(true);
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            bloodPoof(serverLevel, itemEntity.getX(), itemEntity.getY() + 0.2, itemEntity.getZ());
        }
    }

    /** Placing the sword in a chest, barrel, ender chest, shulker box, hopper or dispenser destroys it. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }

        Player player = event.player;
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu
                || menu instanceof HopperMenu || menu instanceof DispenserMenu)) {
            return;
        }

        boolean removed = false;
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) {
                continue; // the player's own inventory is fine
            }
            if (slot.getItem().is(ModItems.ESPADA_DE_SANGUE.get())) {
                slot.set(ItemStack.EMPTY);
                removed = true;
            }
        }

        if (removed && player.level() instanceof ServerLevel serverLevel) {
            bloodPoof(serverLevel, player.getX(), player.getY() + 1.0, player.getZ());
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.espada_dissipada"), true);
        }
    }

    /** When the player dies the sword disappears, even with keepInventory on. */
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

    private static void bloodPoof(ServerLevel level, double x, double y, double z) {
        level.sendParticles(new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.0F), 1.3F),
                x, y, z, 35, 0.3, 0.3, 0.3, 0.03);
        level.playSound(null, x, y, z, ModSounds.BLOOD_SQUELCH.get(), SoundSource.PLAYERS, 0.6F, 1.2F);
    }
}
