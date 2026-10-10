package com.livrodassombras.event;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.entity.BloodBeingEntity;
import com.livrodassombras.entity.TimeSkeletonEntity;
import com.livrodassombras.summon.BloodBond;
import net.minecraft.server.level.ServerPlayer;
import com.livrodassombras.registry.ModSounds;
import com.livrodassombras.summon.BloodWeapons;
import com.livrodassombras.summon.TimeSummoning;
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
 * Every summoned weapon dissipates when it leaves its owner:
 * thrown on the ground, stored in a chest-like container, or when the owner dies.
 */
@Mod.EventBusSubscriber(modid = LivroDasSombras.MOD_ID)
public class WeaponBindingEvents {

    /** Any weapon item entity (Q key, inventory drop, dispenser...) turns into blood particles. */
    @SubscribeEvent
    public static void onItemEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }
        if (!BloodWeapons.is(itemEntity.getItem())) {
            return;
        }
        if (event.getLevel().isClientSide) {
            return;
        }

        event.setCanceled(true);
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (BloodWeapons.isTime(itemEntity.getItem())) {
                TimeSummoning.vanish(serverLevel, itemEntity.getX(), itemEntity.getY() + 0.2, itemEntity.getZ());
            } else {
                bloodPoof(serverLevel, itemEntity.getX(), itemEntity.getY() + 0.2, itemEntity.getZ());
            }
        }
    }

    /** Placing a weapon in a chest, barrel, ender chest, shulker box, hopper or dispenser destroys it. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }

        Player player = event.player;
        if (player.tickCount % 20 == 0 && player instanceof ServerPlayer serverPlayer) {
            BloodBond.check(serverPlayer); // gives sacrificed hearts back if the Ser de Sangue is gone
            TimeSkeletonEntity.check(serverPlayer); // no skeletons left, no timer
        }

        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu
                || menu instanceof HopperMenu || menu instanceof DispenserMenu)) {
            return;
        }

        boolean removed = false;
        boolean time = false;
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) {
                continue; // the player's own inventory is fine
            }
            if (BloodWeapons.is(slot.getItem())) {
                time |= BloodWeapons.isTime(slot.getItem());
                slot.set(ItemStack.EMPTY);
                removed = true;
            }
        }

        if (removed && player.level() instanceof ServerLevel serverLevel) {
            if (time) {
                TimeSummoning.vanish(serverLevel, player.getX(), player.getY() + 1.0, player.getZ());
            } else {
                bloodPoof(serverLevel, player.getX(), player.getY() + 1.0, player.getZ());
            }
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.espada_dissipada"), true);
        }
    }

    /** When the player dies the weapon disappears, even with keepInventory on, and so does their Ser de Sangue. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (BloodWeapons.is(inventory.getItem(i))) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        if (BloodWeapons.is(player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            BloodBeingEntity.dissipateAllOf(serverLevel, player);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            TimeSkeletonEntity.dissipateAllOf(serverPlayer);
        }
    }

    private static void bloodPoof(ServerLevel level, double x, double y, double z) {
        level.sendParticles(new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.0F), 1.3F),
                x, y, z, 35, 0.3, 0.3, 0.3, 0.03);
        level.playSound(null, x, y, z, ModSounds.BLOOD_SQUELCH.get(), SoundSource.PLAYERS, 0.6F, 1.2F);
    }
}
