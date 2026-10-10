package com.livrodassombras.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.livrodassombras.entity.BloodSpearEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Lança de Sangue: 6 damage in melee. Hold right-click and release to throw it;
 * the thrown spear flies back to the player on its own.
 */
public class BloodSpearItem extends Item {
    private static final int MIN_CHARGE_TICKS = 10;
    private static final float THROW_VELOCITY = 2.5F;

    private final Multimap<Attribute, AttributeModifier> attributes;

    public BloodSpearItem(Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        // The player's base damage is 1, so +5 makes 6.
        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 5.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.9D, AttributeModifier.Operation.ADDITION));
        this.attributes = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.attributes : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int charge = this.getUseDuration(stack) - timeLeft;
        if (charge < MIN_CHARGE_TICKS || level.isClientSide) {
            return;
        }

        BloodSpearEntity spear = new BloodSpearEntity(level, player);
        spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_VELOCITY, 1.0F);
        level.addFreshEntity(spear);
        level.playSound(null, spear, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);

        // The spear leaves the hand; the entity brings it back.
        player.getInventory().removeItem(stack);
        player.awardStat(Stats.ITEM_USED.get(this));
    }
}
