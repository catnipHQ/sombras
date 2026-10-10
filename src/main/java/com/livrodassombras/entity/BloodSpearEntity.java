package com.livrodassombras.entity;

import com.livrodassombras.registry.ModEffects;
import com.livrodassombras.registry.ModEntities;
import com.livrodassombras.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The thrown Lança de Sangue. Works like a Trident with Loyalty: it comes back to the owner
 * after hitting something (or after sticking into a block), but always deals exactly 6 damage.
 * A thrown hit also slows the target for 1.5 seconds and makes it bleed for as long (melee hits don't).
 */
public class BloodSpearEntity extends AbstractArrow {
    public static final float DAMAGE = 6.0F;
    private static final int RETURN_STRENGTH = 3; // same pull as Loyalty III
    private static final int SLOW_TICKS = 30;     // 1.5 seconds

    private boolean returning;
    private boolean returnSoundPlayed;

    public BloodSpearEntity(EntityType<? extends BloodSpearEntity> type, Level level) {
        super(type, level);
    }

    public BloodSpearEntity(Level level, LivingEntity owner) {
        super(ModEntities.LANCA_DE_SANGUE.get(), owner, level);
        this.pickup = AbstractArrow.Pickup.ALLOWED;
    }

    @Override
    public void tick() {
        Entity owner = this.getOwner();

        // If the owner dies, logs off or leaves, the spear disappears.
        if (!this.level().isClientSide && (owner == null || !owner.isAlive())) {
            this.discard();
            return;
        }

        // Stuck in a block for a moment: fly back.
        if (this.inGroundTime > 4) {
            this.returning = true;
        }

        if (this.returning && owner != null) {
            this.setNoPhysics(true);
            Vec3 toOwner = owner.getEyePosition().subtract(this.position());
            this.setPosRaw(this.getX(), this.getY() + toOwner.y * 0.015D * RETURN_STRENGTH, this.getZ());
            if (this.level().isClientSide) {
                this.yOld = this.getY();
            }
            double pull = 0.05D * RETURN_STRENGTH;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.95D).add(toOwner.normalize().scale(pull)));

            if (!this.returnSoundPlayed) {
                this.returnSoundPlayed = true;
                this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
            }
        }

        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().trident(this, owner == null ? this : owner);

        if (target.hurt(source, DAMAGE) && target instanceof LivingEntity living) {
            this.doPostHurtEffects(living);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 0, false, false, true), owner);
            living.addEffect(new MobEffectInstance(ModEffects.SANGRANDO.get(), SLOW_TICKS, 0, false, false, true), owner);
        }

        this.returning = true;
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    @Override
    protected boolean tryPickup(Player player) {
        return this.ownedBy(player) && super.tryPickup(player);
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.LANCA_DE_SANGUE.get());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Returning", this.returning);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.returning = tag.getBoolean("Returning");
    }
}
