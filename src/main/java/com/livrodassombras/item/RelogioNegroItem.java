package com.livrodassombras.item;

import com.livrodassombras.registry.ModEffects;
import com.livrodassombras.ritual.MinorRituals;
import com.livrodassombras.ritual.TimeRituals;
import com.livrodassombras.summon.TimeSummoning;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;

/**
 * Relógio Negro: 3 charges. Right-click aiming at a creature up to 12 blocks away stops it in time
 * for 2 seconds (no AI: it can't move, look or attack). Players and bosses only get Slowness III for 3 s.
 * There are 3 s between charges, so the same creature can't be kept frozen; after the last charge the clock vanishes.
 */
public class RelogioNegroItem extends Item {
    public static final int CHARGES = 3;
    public static final double RANGE = 12.0D;
    public static final int FREEZE_TICKS = 40;          // 2 s
    public static final int PLAYER_SLOW_TICKS = 60;     // Slowness III for 3 s
    public static final int COOLDOWN_TICKS = 60;        // 3 s between charges

    public RelogioNegroItem(Properties properties) {
        super(properties.durability(CHARGES));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }

        LivingEntity target = MinorRituals.findTarget(player, RANGE);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.ritual.sem_alvo", (int) RANGE), true);
            return InteractionResultHolder.fail(stack);
        }

        if (target instanceof Player || target.getType().is(Tags.EntityTypes.BOSSES)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PLAYER_SLOW_TICKS, 2), player);
        } else {
            target.addEffect(new MobEffectInstance(ModEffects.PARALISIA.get(), FREEZE_TICKS, 0, false, false, true), player);
        }

        serverLevel.sendParticles(TimeRituals.DARK, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                25, target.getBbWidth() * 0.5D, target.getBbHeight() * 0.4D, target.getBbWidth() * 0.5D, 0.0D);
        serverLevel.sendParticles(TimeRituals.GREY, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                15, target.getBbWidth() * 0.5D, target.getBbHeight() * 0.4D, target.getBbWidth() * 0.5D, 0.0D);
        serverLevel.playSound(null, target.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1.0F, 0.5F);
        serverLevel.playSound(null, target.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.5F, 0.5F);

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        stack.hurtAndBreak(1, player, p -> {
            p.broadcastBreakEvent(hand);
            TimeSummoning.poof(serverLevel, p.getX(), p.getY() + 1.0D, p.getZ());
        });
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.livro_das_sombras.cargas", stack.getMaxDamage() - stack.getDamageValue(), CHARGES)
                .withStyle(ChatFormatting.GRAY));
    }
}
