package com.livrodassombras.item;

import com.livrodassombras.registry.ModItems;
import com.livrodassombras.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** Right-click to summon the Espada de Sangue (costs 5 hearts + 2 seconds of Nausea). */
public class LivroDasSombrasItem extends Item {
    private static final int COOLDOWN_TICKS = 20 * 5;  // 5 seconds
    private static final float BLOOD_COST = 10.0F;     // 10 health points = 5 hearts
    private static final int NAUSEA_TICKS = 40;        // 2 seconds

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
            warn(player, "message.livro_das_sombras.ja_invocada");
            return InteractionResultHolder.fail(book);
        }

        // Dropped swords vanish, so there must be room for it in the inventory.
        if (player.getInventory().getFreeSlot() == -1) {
            warn(player, "message.livro_das_sombras.inventario_cheio");
            return InteractionResultHolder.fail(book);
        }

        boolean creative = player.getAbilities().instabuild;

        // The summoning must never kill the player.
        if (!creative && player.getHealth() <= BLOOD_COST) {
            warn(player, "message.livro_das_sombras.sem_sangue");
            return InteractionResultHolder.fail(book);
        }

        // Blood price: 5 hearts (ignores armor, creative players take no damage).
        if (!creative && !player.hurt(level.damageSources().magic(), BLOOD_COST)) {
            return InteractionResultHolder.fail(book);
        }
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS));

        player.getInventory().add(new ItemStack(ModItems.ESPADA_DE_SANGUE.get()));

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

    private static void warn(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
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
