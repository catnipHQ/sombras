package com.livrodassombras.event;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.block.RitualCauldronBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The Caldeirão Ritualístico's sacrifice: a creature a player kills on or right next to it. */
@Mod.EventBusSubscriber(modid = LivroDasSombras.MOD_ID)
public class CauldronEvents {

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = false)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)
                || victim instanceof Player
                || victim instanceof ArmorStand) {
            return;
        }
        // Killed by a player, by hand or with a projectile.
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        RitualCauldronBlock.onSacrifice(level, victim);
    }
}
