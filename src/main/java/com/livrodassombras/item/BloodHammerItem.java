package com.livrodassombras.item;

import com.livrodassombras.summon.Allegiance;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.livrodassombras.registry.ModSounds;
import com.livrodassombras.summon.BloodPrice;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import org.joml.Vector3f;

/**
 * Martelo de Sangue: 10 damage in melee, but slow (attack speed 0.6).
 * Hold right-click to raise it; once charged (1 second), releasing slams the ground:
 * every living thing within 4 blocks takes 5 damage and is knocked back, and the player pays 3 health.
 */
public class BloodHammerItem extends Item {
    private static final double RADIUS = 4.0D;
    private static final float AOE_DAMAGE = 5.0F;
    private static final float SLAM_COST = 3.0F;      // 3 health points = 1.5 hearts
    private static final int CHARGE_TICKS = 20;        // 1 second
    private static final int AOE_COOLDOWN_TICKS = 20 * 2; // 2 seconds

    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.0F), 1.4F);
    private static final DustParticleOptions DARK_BLOOD = new DustParticleOptions(new Vector3f(0.35F, 0.0F, 0.0F), 1.0F);

    private final Multimap<Attribute, AttributeModifier> attributes;

    public BloodHammerItem(Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        // The player's base damage is 1, so +9 makes 10. Base attack speed is 4, so -3.4 makes 0.6.
        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 9.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -3.4D, AttributeModifier.Operation.ADDITION));
        this.attributes = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.attributes : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR; // hammer raised over the head while charging
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BloodPrice.canPay(player, SLAM_COST)) {
            if (!level.isClientSide) {
                BloodPrice.warnNotEnough(player);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** While charging: blood gathers around the player, and a heavy sound marks when the slam is ready. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int charge = this.getUseDuration(stack) - remainingUseDuration;

        if (charge < CHARGE_TICKS && charge % 3 == 0) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
            double r = 1.2D - charge / (double) CHARGE_TICKS;
            serverLevel.sendParticles(DARK_BLOOD,
                    entity.getX() + Math.cos(angle) * r, entity.getY() + 0.3D, entity.getZ() + Math.sin(angle) * r,
                    2, 0.05D, 0.2D, 0.05D, 0.0D);
        }

        if (charge == CHARGE_TICKS) {
            serverLevel.playSound(null, entity.blockPosition(), ModSounds.BLOOD_SQUELCH.get(), SoundSource.PLAYERS, 1.0F, 0.5F);
            serverLevel.playSound(null, entity.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.6F, 0.7F);
            serverLevel.sendParticles(BLOOD, entity.getX(), entity.getY() + 2.1D, entity.getZ(), 12, 0.25D, 0.15D, 0.25D, 0.0D);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int charge = this.getUseDuration(stack) - timeLeft;
        if (charge < CHARGE_TICKS) {
            return; // released too early: nothing happens
        }

        // The slam drains the wielder.
        if (!BloodPrice.pay(player, SLAM_COST)) {
            BloodPrice.warnNotEnough(player);
            return;
        }

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RADIUS),
                target -> target != player
                        && target.isAlive()
                        && !target.isSpectator()
                        && player.distanceToSqr(target) <= RADIUS * RADIUS
                        && !Allegiance.belongsTo(target, player));

        for (LivingEntity target : targets) {
            if (target.hurt(serverLevel.damageSources().playerAttack(player), AOE_DAMAGE)) {
                target.knockback(0.8D, player.getX() - target.getX(), player.getZ() - target.getZ());
            }
        }

        // Shockwave ring on the ground.
        for (int i = 0; i < 48; i++) {
            double angle = (Math.PI * 2.0D) * i / 48.0D;
            serverLevel.sendParticles(BLOOD,
                    player.getX() + Math.cos(angle) * RADIUS, player.getY() + 0.15D, player.getZ() + Math.sin(angle) * RADIUS,
                    2, 0.1D, 0.05D, 0.1D, 0.0D);
        }
        serverLevel.sendParticles(BLOOD, player.getX(), player.getY() + 0.2D, player.getZ(), 40, 1.2D, 0.1D, 1.2D, 0.02D);

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.6F);
        serverLevel.playSound(null, player.blockPosition(), ModSounds.BLOOD_SQUELCH.get(), SoundSource.PLAYERS, 1.0F, 0.7F);

        player.getCooldowns().addCooldown(this, AOE_COOLDOWN_TICKS);
    }
}
