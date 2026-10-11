package com.livrodassombras.item;

import com.livrodassombras.summon.BloodPrice;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Arco de Sangue: needs no arrows, but every shot costs 1 heart.
 * Fully drawn, the arrow deals 7 damage and is a critical arrow (crits add vanilla's random bonus).
 * The arrow can't be picked up.
 */
public class BloodBowItem extends BowItem {
    /** Arrow damage is ceil(speed * base). Full draw is speed 3, so 2.25 gives 6.75, rounded up to 7. */
    private static final double ARROW_BASE_DAMAGE = 2.25D;
    /** 2 health points = 1 heart per arrow. */
    private static final float SHOT_COST = 2.0F;

    public BloodBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // No arrow check: the bow always draws. The blood price is checked when the arrow is released.
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || level.isClientSide) {
            return;
        }

        int charge = this.getUseDuration(stack) - timeLeft;
        float power = getPowerForTime(charge);
        if (power < 0.1F) {
            return;
        }

        if (!BloodPrice.pay(player, SHOT_COST)) {
            BloodPrice.warnNotEnough(player);
            return;
        }

        Arrow arrow = new Arrow(level, player);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F, 1.0F);
        arrow.setBaseDamage(ARROW_BASE_DAMAGE);
        if (power >= 1.0F) {
            arrow.setCritArrow(true);
        }
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        level.addFreshEntity(arrow);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }
}
