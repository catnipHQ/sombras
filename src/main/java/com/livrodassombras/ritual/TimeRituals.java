package com.livrodassombras.ritual;

import com.livrodassombras.summon.Allegiance;
import java.util.function.Consumer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The Livro do Tempo's minor rituals. Server side only; costs and cooldowns are handled by {@link MinorRituals#cast}. */
public final class TimeRituals {
    // Lentidão
    public static final double SLOW_RANGE = 12.0D;
    public static final int SLOW_CAST = 20;            // 1 s to cast
    public static final int SLOW_TICKS = 20 * 8;       // Slowness II for 8 s
    public static final double SLOW_SPLASH = 2.5D;     // also catches what stands right next to the target

    // Murchar
    public static final double WITHER_RANGE = 10.0D;
    public static final int WITHER_CAST = 30;          // 1.5 s to cast
    public static final int WITHER_TICKS = 20 * 6;     // Wither II (6 damage) and Weakness I for 6 s

    public static final DustParticleOptions GREY = new DustParticleOptions(new Vector3f(0.82F, 0.82F, 0.84F), 1.1F);
    public static final DustParticleOptions DARK = new DustParticleOptions(new Vector3f(0.12F, 0.12F, 0.14F), 1.1F);

    private TimeRituals() {}

    // ---------------------------------------------------------------- Lentidão

    static boolean castSlow(ServerPlayer player, ServerLevel level) {
        LivingEntity target = MinorRituals.findTarget(player, SLOW_RANGE);
        if (target == null) {
            MinorRituals.noTarget(player, SLOW_RANGE);
            return false;
        }
        if (!MinorRituals.payPrice(player, MinorRitual.LENTIDAO)) {
            return false;
        }
        RitualTasks.add(new CastTask(player, target, level, SLOW_CAST, SLOW_RANGE, landed -> {
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, landed.getBoundingBox().inflate(SLOW_SPLASH),
                    e -> e.isAlive() && !e.isSpectator() && (e == landed || !isFriendly(player, e)))) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 1), player);
                level.sendParticles(GREY, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                        12, victim.getBbWidth() * 0.4D, victim.getBbHeight() * 0.3D, victim.getBbWidth() * 0.4D, 0.0D);
            }
            level.playSound(null, landed.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 0.7F);
        }));
        return true;
    }

    // ---------------------------------------------------------------- Murchar

    static boolean castWither(ServerPlayer player, ServerLevel level) {
        LivingEntity target = MinorRituals.findTarget(player, WITHER_RANGE);
        if (target == null) {
            MinorRituals.noTarget(player, WITHER_RANGE);
            return false;
        }
        if (!MinorRituals.payPrice(player, MinorRitual.MURCHAR)) {
            return false;
        }
        RitualTasks.add(new CastTask(player, target, level, WITHER_CAST, WITHER_RANGE, landed -> {
            landed.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_TICKS, 1), player);
            landed.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WITHER_TICKS, 0), player);
            level.sendParticles(ParticleTypes.SMOKE, landed.getX(), landed.getY() + landed.getBbHeight() * 0.6D, landed.getZ(),
                    25, landed.getBbWidth() * 0.4D, landed.getBbHeight() * 0.3D, landed.getBbWidth() * 0.4D, 0.02D);
            level.playSound(null, landed.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.5F, 0.6F);
        }));
        return true;
    }

    // ---------------------------------------------------------------- Reverter Passos

    /**
     * No mark yet: marks the spot (free, no cooldown, no use of the book) and returns false.
     * With a mark: brings the player back to it, and that counts as the cast.
     */
    static boolean castRevert(ServerPlayer player, ServerLevel level) {
        if (!RevertMarks.has(player)) {
            RevertMarks.mark(player);
            return false;
        }
        return RevertMarks.returnTo(player, level);
    }

    // ---------------------------------------------------------------- shared

    /** The caster, their tamed animals and their summoned creatures. */
    static boolean isFriendly(Player caster, LivingEntity entity) {
        return Allegiance.sameSide(entity, caster);
    }

    /**
     * A ritual that takes a moment to cast: a grey thread runs from the caster to the target while
     * a clock-face ring tightens around it. If the target dies, leaves, or gets too far away, it fizzles.
     */
    private static final class CastTask implements RitualTasks.Task {
        private final ServerPlayer caster;
        private final LivingEntity target;
        private final ServerLevel level;
        private final int castTicks;
        private final double maxDistanceSqr;
        private final Consumer<LivingEntity> onLand;
        private int age;

        CastTask(ServerPlayer caster, LivingEntity target, ServerLevel level, int castTicks, double range,
                 Consumer<LivingEntity> onLand) {
            this.caster = caster;
            this.target = target;
            this.level = level;
            this.castTicks = castTicks;
            this.maxDistanceSqr = range * 1.5D * range * 1.5D;
            this.onLand = onLand;
            level.playSound(null, caster.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.7F, 0.5F);
        }

        @Override
        public boolean tick() {
            if (this.target.isRemoved() || !this.target.isAlive() || this.target.level() != this.level
                    || this.caster.isRemoved() || !this.caster.isAlive() || this.caster.level() != this.level
                    || this.caster.distanceToSqr(this.target) > this.maxDistanceSqr) {
                this.level.sendParticles(ParticleTypes.SMOKE, this.target.getX(), this.target.getY() + 1.0D, this.target.getZ(),
                        6, 0.2D, 0.3D, 0.2D, 0.01D);
                return true; // fizzled
            }
            this.age++;
            double x = this.target.getX();
            double y = this.target.getY();
            double z = this.target.getZ();
            double h = this.target.getBbHeight();
            double r = Math.max(0.6D, this.target.getBbWidth() * 0.9D) * (1.7D - this.age / (double) this.castTicks);

            // Twelve hour marks around the target, closing in; the two "hands" sweep around.
            if (this.age % 2 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6.0D;
                    this.level.sendParticles(GREY, x + Math.cos(a) * r, y + h * 0.5D, z + Math.sin(a) * r, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            double hand = this.age * 0.6D;
            this.level.sendParticles(DARK, x + Math.cos(hand) * r * 0.6D, y + h * 0.5D, z + Math.sin(hand) * r * 0.6D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (this.age % 4 == 0) {
                Vec3 from = this.caster.position().add(0.0D, this.caster.getBbHeight() * 0.65D, 0.0D);
                Vec3 to = this.target.position().add(0.0D, h * 0.5D, 0.0D);
                Vec3 p = from.lerp(to, (this.age % 20) / 20.0D);
                this.level.sendParticles(GREY, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
            if (this.age % 10 == 0) {
                this.level.playSound(null, this.target.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS,
                        0.5F, 0.5F + this.age / (float) this.castTicks * 0.5F);
            }

            if (this.age >= this.castTicks) {
                this.onLand.accept(this.target);
                return true;
            }
            return false;
        }
    }

    /** Action-bar message shortcut. */
    static void say(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }
}
