package com.livrodassombras.item;

import com.livrodassombras.registry.ModItems;
import com.livrodassombras.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** Right-click to summon the Espada de Sangue. */
public class LivroDasSombrasItem extends Item {
    private static final int COOLDOWN_TICKS = 20 * 5; // 5 seconds

    public LivroDasSombrasItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(book, true);
        }

        // Only one sword at a time.
        if (hasSword(player)) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.ja_invocada"), true);
            return InteractionResultHolder.fail(book);
        }

        ItemStack sword = new ItemStack(ModItems.ESPADA_DE_SANGUE.get());
        if (!player.getInventory().add(sword)) {
            player.drop(sword, false);
        }

        // Blood magic sound (two layers) and red particles.
        level.playSound(null, player.blockPosition(), ModSounds.BLOOD_SUMMON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.blockPosition(), ModSounds.BLOOD_SQUELCH.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.0F), 1.2F),
                    player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 0.7, 0.5, 0.02);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.displayClientMessage(Component.translatable("message.livro_das_sombras.invocada"), true);
        return InteractionResultHolder.sidedSuccess(book, false);
    }

    private static boolean hasSword(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.ESPADA_DE_SANGUE.get())) {
                return true;
            }
        }
        return false;
    }
}
