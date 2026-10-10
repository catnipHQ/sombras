package com.livrodassombras.ritual;

import com.livrodassombras.registry.ModParticles;
import com.livrodassombras.registry.ModSounds;
import com.livrodassombras.summon.BloodPrice;
import com.livrodassombras.summon.Bloodless;
import com.livrodassombras.summon.TimePrice;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Casting the minor rituals. Server side only. */
public final class MinorRituals {
    // Dilacerar
    public static final double TEAR_RANGE = 8.0D;
    public static final int TEAR_DELAY = 20;          // 1 s of warning
    public static final int TEAR_DURATION = 20 * 6;   // 6 s
    public static final int TEAR_INTERVAL = 10;       // a hit every half second
    public static final float TEAR_DAMAGE = 1.0F;     // 12 damage (6 hearts) in total

    // Troca de sangue
    public static final double TRADE_RANGE = 8.0D;
    public static final float TRADE_HEAL = 8.0F;      // 4 hearts

    // Corte sangrento
    public static final double SLASH_RANGE = 10.0D;
    public static final float SLASH_DAMAGE = 14.0F;   // 7 hearts, armor still counts
    private static final double SLASH_SPEED = 2.5D;   // blocks per tick
    private static final double SLASH_HALF_WIDTH = 1.1D;

    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.0F), 1.2F);
    private static final DustParticleOptions BRIGHT_BLOOD = new DustParticleOptions(new Vector3f(0.95F, 0.05F, 0.05F), 1.5F);
    private static final DustParticleOptions DARK_BLOOD = new DustParticleOptions(new Vector3f(0.35F, 0.0F, 0.0F), 1.0F);
    private static final DustParticleOptions BONE = new DustParticleOptions(new Vector3f(0.9F, 0.88F, 0.8F), 1.0F);

    private MinorRituals() {}

    /** Shift + right-click: the book goes back to normal. */
    public static void cancel(Player player, ItemStack book) {
        if (RitualBook.charged(book) == null) {
            return;
        }
        RitualBook.discharge(book);
        player.displayClientMessage(Component.translatable("message.livro_das_sombras.ritual.cancelado"), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /** Casts the ritual. On success it costs one use of the book and starts the ritual's cooldown. */
    public static void cast(ServerPlayer player, ItemStack book, InteractionHand hand, MinorRitual ritual) {
        ServerLevel level = player.serverLevel();
        if (RitualCooldowns.left(player, ritual) > 0L) {
            // The cooldown bar in the corner already says how long; just a dull click.
            level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 0.6F);
            return;
        }

        // Reverter Passos: marking the spot is free; only the way back counts as a cast.
        boolean cast = switch (ritual) {
            case DILACERAR -> castTear(player, level);
            case TROCA_DE_SANGUE -> castTrade(player, level);
            case CORTE_SANGRENTO -> castSlash(player, level);
            case LENTIDAO -> TimeRituals.castSlow(player, level);
            case MURCHAR -> TimeRituals.castWither(player, level);
            case REVERTER_PASSOS -> TimeRituals.castRevert(player, level);
        };
        if (cast && !player.getAbilities().instabuild) {
            RitualCooldowns.start(player, ritual);
            book.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
    }

    // ---------------------------------------------------------------- Dilacerar

    private static boolean castTear(ServerPlayer player, ServerLevel level) {
        LivingEntity target = findTarget(player, TEAR_RANGE);
        if (target == null) {
            noTarget(player, TEAR_RANGE);
            return false;
        }
        // Bone creatures have no blood to tear (nothing is charged).
        if (Bloodless.is(target)) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.ritual.sem_sangue_alvo",
                    target.getDisplayName()), true);
            level.playSound(null, player.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 0.6F, 0.6F);
            return false;
        }
        if (!payPrice(player, MinorRitual.DILACERAR)) {
            return false;
        }
        level.playSound(null, target.blockPosition(), ModSounds.BLOOD_SUMMON.get(), SoundSource.PLAYERS, 1.0F, 1.5F);
        RitualTasks.add(new TearTask(player, target, level));
        return true;
    }

    private static final class TearTask implements RitualTasks.Task {
        private final ServerPlayer caster;
        private final LivingEntity target;
        private final ServerLevel level;
        private int age;

        TearTask(ServerPlayer caster, LivingEntity target, ServerLevel level) {
            this.caster = caster;
            this.target = target;
            this.level = level;
        }

        @Override
        public boolean tick() {
            if (this.target.isRemoved() || !this.target.isAlive() || this.target.level() != this.level) {
                return true;
            }
            this.age++;
            double x = this.target.getX();
            double y = this.target.getY();
            double z = this.target.getZ();
            double h = this.target.getBbHeight();
            double r = Math.max(0.6D, this.target.getBbWidth() * 0.9D);

            if (this.age <= TEAR_DELAY) {
                // Warning: blood circling the target, tightening.
                double radius = r * (1.6D - this.age / (double) TEAR_DELAY);
                for (int i = 0; i < 3; i++) {
                    double a = this.age * 0.5D + i * Math.PI * 2.0D / 3.0D;
                    this.level.sendParticles(BLOOD, x + Math.cos(a) * radius, y + h * (this.age / (double) TEAR_DELAY),
                            z + Math.sin(a) * radius, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                if (this.age == TEAR_DELAY) {
                    this.level.playSound(null, this.target.blockPosition(), ModSounds.BLOOD_SQUELCH.get(),
                            SoundSource.PLAYERS, 1.2F, 0.5F);
                    this.level.playSound(null, this.target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                            SoundSource.PLAYERS, 0.8F, 0.6F);
                    this.level.sendParticles(BRIGHT_BLOOD, x, y + h * 0.6D, z, 30, r * 0.4D, h * 0.3D, r * 0.4D, 0.05D);
                }
                return false;
            }

            int t = this.age - TEAR_DELAY;
            if (t > TEAR_DURATION) {
                return true;
            }
            if (t % 2 == 0) {
                this.level.sendParticles(BLOOD, x, y + h * 0.55D, z, 3, r * 0.35D, h * 0.25D, r * 0.35D, 0.04D);
                this.level.sendParticles(DARK_BLOOD, x, y + h * 0.3D, z, 1, r * 0.3D, h * 0.15D, r * 0.3D, 0.0D);
            }
            if (t % 4 == 0 && this.target.onGround()) {
                this.level.sendParticles(ModParticles.BLOOD_TRAIL.get(), x, y + 0.02D, z, 1, r * 0.3D, 0.0D, r * 0.3D, 0.0D);
            }
            if (t % TEAR_INTERVAL == 0) {
                DamageSource source = this.caster.isRemoved()
                        ? this.level.damageSources().magic()
                        : this.level.damageSources().indirectMagic(this.caster, this.caster);
                hurtIgnoringCooldown(this.target, source, TEAR_DAMAGE);
            }
            return false;
        }
    }

    // ---------------------------------------------------------------- Troca de sangue

    private static boolean castTrade(ServerPlayer player, ServerLevel level) {
        LivingEntity target = findTarget(player, TRADE_RANGE);
        if (target == null) {
            noTarget(player, TRADE_RANGE);
            return false;
        }
        if (target.getHealth() >= target.getMaxHealth()) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.ritual.vida_cheia",
                    target.getDisplayName()), true);
            return false;
        }
        if (!payPrice(player, MinorRitual.TROCA_DE_SANGUE)) {
            return false;
        }
        target.heal(TRADE_HEAL);

        // A stream of blood from the caster to the target.
        Vec3 from = player.position().add(0.0D, player.getBbHeight() * 0.65D, 0.0D);
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
        int steps = Math.max(8, (int) (from.distanceTo(to) * 4.0D));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(BLOOD, p.x, p.y, p.z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
        level.sendParticles(ParticleTypes.HEART, to.x, to.y + 0.4D, to.z, 4, 0.3D, 0.2D, 0.3D, 0.0D);
        level.playSound(null, player.blockPosition(), ModSounds.BLOOD_SQUELCH.get(), SoundSource.PLAYERS, 0.9F, 0.8F);
        level.playSound(null, target.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8F, 1.3F);
        if (target instanceof Player other) {
            other.displayClientMessage(Component.translatable("message.livro_das_sombras.ritual.troca_recebida",
                    player.getDisplayName()), true);
        }
        return true;
    }

    // ---------------------------------------------------------------- Corte sangrento

    private static boolean castSlash(ServerPlayer player, ServerLevel level) {
        if (!payPrice(player, MinorRitual.CORTE_SANGRENTO)) {
            return false;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.5F);
        level.playSound(null, player.blockPosition(), ModSounds.BLOOD_SQUELCH.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
        RitualTasks.add(new SlashTask(player, level));
        return true;
    }

    private static final class SlashTask implements RitualTasks.Task {
        private final ServerPlayer caster;
        private final ServerLevel level;
        private final Vec3 origin;
        private final Vec3 dir;
        private final Vec3 right;
        private final double length;
        private final Set<Integer> hit = new HashSet<>();
        private double travelled;

        SlashTask(ServerPlayer caster, ServerLevel level) {
            this.caster = caster;
            this.level = level;
            this.origin = caster.getEyePosition().subtract(0.0D, 0.4D, 0.0D);
            this.dir = caster.getLookAngle().normalize();
            Vec3 side = this.dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
            this.right = side.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
            // The slash stops at the first solid block.
            Vec3 end = this.origin.add(this.dir.scale(SLASH_RANGE));
            BlockHitResult wall = level.clip(new ClipContext(this.origin, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, caster));
            this.length = wall.getType() == HitResult.Type.MISS ? SLASH_RANGE : wall.getLocation().distanceTo(this.origin);
        }

        @Override
        public boolean tick() {
            double from = this.travelled;
            double to = Math.min(this.length, from + SLASH_SPEED);

            // The crescent of blood, drawn every half block.
            for (double d = from; d <= to; d += 0.5D) {
                Vec3 c = this.origin.add(this.dir.scale(d));
                for (int k = -4; k <= 4; k++) {
                    Vec3 p = c.add(this.right.scale(k * 0.25D)).add(this.dir.scale(-k * k * 0.035D));
                    this.level.sendParticles(Math.abs(k) <= 1 ? BRIGHT_BLOOD : BLOOD, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            Vec3 mid = this.origin.add(this.dir.scale((from + to) / 2.0D));
            this.level.sendParticles(ParticleTypes.SWEEP_ATTACK, mid.x, mid.y, mid.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

            // Everything the slash crosses on this stretch.
            Vec3 a = this.origin.add(this.dir.scale(from));
            Vec3 b = this.origin.add(this.dir.scale(to));
            AABB box = new AABB(a, b).inflate(SLASH_HALF_WIDTH + 1.0D);
            for (LivingEntity target : this.level.getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
                Vec3 centre = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
                double reach = SLASH_HALF_WIDTH + target.getBbWidth() / 2.0D + target.getBbHeight() / 4.0D;
                if (distanceToSegment(centre, a, b) > reach || !this.hit.add(target.getId())) {
                    continue;
                }
                DamageSource source = this.caster.isRemoved()
                        ? this.level.damageSources().magic()
                        : this.level.damageSources().playerAttack(this.caster);
                if (hurtIgnoringCooldown(target, source, SLASH_DAMAGE)) {
                    target.knockback(0.6D, -this.dir.x, -this.dir.z);
                }
                if (Bloodless.is(target)) {
                    // No blood in bone: chips of bone instead.
                    this.level.sendParticles(BONE, centre.x, centre.y, centre.z, 20, 0.3D, 0.4D, 0.3D, 0.05D);
                    this.level.playSound(null, target.blockPosition(), SoundEvents.BONE_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
                } else {
                    this.level.sendParticles(BRIGHT_BLOOD, centre.x, centre.y, centre.z, 20, 0.3D, 0.4D, 0.3D, 0.05D);
                    this.level.playSound(null, target.blockPosition(), ModSounds.BLOOD_SQUELCH.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                }
            }

            this.travelled = to;
            return this.travelled >= this.length;
        }

        private boolean canHit(LivingEntity target) {
            return target != this.caster && target.isAlive() && !target.isSpectator()
                    && !(target instanceof TamableAnimal pet && pet.isOwnedBy(this.caster));
        }
    }

    // ---------------------------------------------------------------- helpers

    /** The living thing the player is looking at, up to {@code range} blocks away and not behind a wall. */
    @Nullable
    public static LivingEntity findTarget(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult wall = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator() && entity.isPickable(),
                range * range);
        Entity entity = hit == null ? null : hit.getEntity();
        return entity instanceof LivingEntity living ? living : null;
    }

    /** Damage that doesn't use up (or get blocked by) the target's hurt cooldown. */
    static boolean hurtIgnoringCooldown(LivingEntity target, DamageSource source, float amount) {
        int cooldown = target.invulnerableTime;
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(source, amount);
        target.invulnerableTime = Math.min(target.invulnerableTime, cooldown);
        return hurt;
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lengthSqr = ab.lengthSqr();
        double t = lengthSqr < 1.0E-6D ? 0.0D : Math.max(0.0D, Math.min(1.0D, p.subtract(a).dot(ab) / lengthSqr));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /** Health on the Blood path, hunger on the Time path. Never kills, creative players pay nothing. */
    static boolean payPrice(Player player, MinorRitual ritual) {
        boolean paid = ritual.path() == BookPath.TIME
                ? TimePrice.pay(player, (int) ritual.cost())
                : BloodPrice.pay(player, ritual.cost());
        if (!paid) {
            player.displayClientMessage(Component.translatable(ritual.path() == BookPath.TIME
                    ? "message.livro_das_sombras.ritual_sem_fome"
                    : "message.livro_das_sombras.ritual_sem_sangue"), true);
            return false;
        }
        return true;
    }

    static void noTarget(Player player, double range) {
        player.displayClientMessage(Component.translatable("message.livro_das_sombras.ritual.sem_alvo", (int) range), true);
    }
}
