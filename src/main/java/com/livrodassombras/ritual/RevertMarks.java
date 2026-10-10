package com.livrodassombras.ritual;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.network.RevertMarkSyncPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Reverter Passos: the spot the player marked, kept in the player's data.
 * <ul>
 *   <li>The mark lasts {@link #LIFETIME_TICKS} and only works within {@link #RANGE} blocks, in the same dimension.</li>
 *   <li>Past {@link #WARN_RANGE} the player is warned (action bar and a low tick); past {@link #RANGE} the mark breaks.</li>
 *   <li>Only the owner sees the mark (faint grey particles) and the distance on the HUD.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LivroDasSombras.MOD_ID)
public final class RevertMarks {
    public static final double RANGE = 20.0D;
    public static final double WARN_RANGE = 16.0D;
    public static final int LIFETIME_TICKS = 20 * 60 * 5; // 5 minutes

    private static final String TAG = "veritates_passo";

    private RevertMarks() {}

    public static boolean has(ServerPlayer player) {
        return player.getPersistentData().contains(TAG);
    }

    public static void mark(ServerPlayer player) {
        CompoundTag mark = new CompoundTag();
        mark.putString("Dim", player.level().dimension().location().toString());
        mark.putDouble("X", player.getX());
        mark.putDouble("Y", player.getY());
        mark.putDouble("Z", player.getZ());
        mark.putLong("Ends", player.level().getGameTime() + LIFETIME_TICKS);
        player.getPersistentData().put(TAG, mark);
        sync(player);

        ServerLevel level = player.serverLevel();
        level.sendParticles(TimeRituals.GREY, player.getX(), player.getY() + 0.1D, player.getZ(), 30, 0.4D, 0.05D, 0.4D, 0.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 0.8F);
        TimeRituals.say(player, "message.livro_das_sombras.passo.marcado", (int) RANGE);
    }

    /** Takes the player back to the mark. Returns true if it happened. */
    public static boolean returnTo(ServerPlayer player, ServerLevel level) {
        CompoundTag mark = player.getPersistentData().getCompound(TAG);
        Vec3 to = new Vec3(mark.getDouble("X"), mark.getDouble("Y"), mark.getDouble("Z"));

        // Something was built on the spot: refuse, nothing charged, the mark stays.
        if (!level.noCollision(player, player.getBoundingBox().move(to.subtract(player.position())))) {
            TimeRituals.say(player, "message.livro_das_sombras.passo.bloqueado");
            return false;
        }
        if (!MinorRituals.payPrice(player, MinorRitual.REVERTER_PASSOS)) {
            return false;
        }

        Vec3 from = player.position();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1.0D, from.z, 40, 0.3D, 0.6D, 0.3D, 0.05D);
        player.stopRiding();
        player.teleportTo(level, to.x, to.y, to.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1.0D, to.z, 40, 0.3D, 0.6D, 0.3D, 0.05D);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.6F);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.6F);

        clear(player);
        TimeRituals.say(player, "message.livro_das_sombras.passo.voltou");
        return true;
    }

    public static void clear(ServerPlayer player) {
        if (player.getPersistentData().contains(TAG)) {
            player.getPersistentData().remove(TAG);
            sync(player);
        }
    }

    public static void sync(ServerPlayer player) {
        CompoundTag mark = player.getPersistentData().getCompound(TAG);
        RevertMarkSyncPacket packet = has(player)
                ? new RevertMarkSyncPacket(true, mark.getDouble("X"), mark.getDouble("Y"), mark.getDouble("Z"), mark.getLong("Ends"))
                : new RevertMarkSyncPacket(false, 0.0D, 0.0D, 0.0D, 0L);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** Every tick: expiry, range warning, breaking, and the owner-only marker. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER
                || !(event.player instanceof ServerPlayer player) || !has(player)) {
            return;
        }
        CompoundTag mark = player.getPersistentData().getCompound(TAG);
        ServerLevel level = player.serverLevel();

        if (!level.dimension().location().toString().equals(mark.getString("Dim"))) {
            clear(player);
            TimeRituals.say(player, "message.livro_das_sombras.passo.dimensao");
            return;
        }
        if (level.getGameTime() >= mark.getLong("Ends")) {
            clear(player);
            TimeRituals.say(player, "message.livro_das_sombras.passo.expirou");
            return;
        }

        Vec3 spot = new Vec3(mark.getDouble("X"), mark.getDouble("Y"), mark.getDouble("Z"));
        double distance = player.position().distanceTo(spot);
        if (distance > RANGE) {
            clear(player);
            TimeRituals.say(player, "message.livro_das_sombras.passo.quebrou");
            level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 0.8F, 0.4F);
            return;
        }

        // Crossing into the edge zone warns once; it re-arms after coming a few blocks back.
        boolean warned = mark.getBoolean("Warned");
        if (distance > WARN_RANGE && !warned) {
            mark.putBoolean("Warned", true);
            TimeRituals.say(player, "message.livro_das_sombras.passo.aviso", (int) Math.ceil(RANGE - distance));
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.5F);
        } else if (distance < WARN_RANGE - 2.0D && warned) {
            mark.putBoolean("Warned", false);
        }

        if (player.tickCount % 10 == 0) {
            level.sendParticles(player, TimeRituals.GREY, true, spot.x, spot.y + 0.1D, spot.z, 3, 0.25D, 0.02D, 0.25D, 0.0D);
            level.sendParticles(player, ParticleTypes.REVERSE_PORTAL, true, spot.x, spot.y + 0.6D, spot.z, 1, 0.1D, 0.3D, 0.1D, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}
