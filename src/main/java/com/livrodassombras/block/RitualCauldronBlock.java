package com.livrodassombras.block;

import com.livrodassombras.client.ClientHooks;
import com.livrodassombras.registry.ModBlocks;
import com.livrodassombras.registry.ModSounds;
import com.livrodassombras.summon.BloodPrice;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.joml.Vector3f;

/**
 * Caldeirão Ritualístico: the altar that makes the books.
 * <ol>
 *   <li>Light it with flint and steel.</li>
 *   <li>Right-click it: the player gives 4 hearts, it partly fills with blood and blood stains the ground around it.</li>
 *   <li>A creature must be killed on top of it or within one block of it (see {@link #onSacrifice}).</li>
 *   <li>Throw meat into it, one piece at a time, until it reaches 100% (meat with more saturation fills more).</li>
 *   <li>Right-click the full cauldron: a menu to choose the Livro do Sangue or the Livro das Sombras.
 *       Choosing a book empties and puts out the cauldron, so every book costs a whole ritual.</li>
 * </ol>
 */
public class RitualCauldronBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** 0 empty, 1 blood, 2 and 3 filling with meat, 4 complete. */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);
    /** The blood got its creature; only then does it take meat. */
    public static final BooleanProperty SACRIFICED = BooleanProperty.create("sacrificed");
    public static final int COMPLETE = 4;

    public static final float BLOOD_PRICE = 8.0F; // 4 hearts
    /** How far the blood stains reach around the cauldron. */
    private static final int STAIN_RADIUS = 2;

    private static final VoxelShape INSIDE = box(2.0D, 4.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(),
            Shapes.or(box(0.0D, 0.0D, 4.0D, 16.0D, 3.0D, 12.0D),
                    box(4.0D, 0.0D, 0.0D, 12.0D, 3.0D, 16.0D),
                    box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D),
                    INSIDE),
            BooleanOp.ONLY_FIRST);

    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.6F, 0.0F, 0.0F), 1.2F);

    public RitualCauldronBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false).setValue(LEVEL, 0).setValue(SACRIFICED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, LEVEL, SACRIFICED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // BaseEntityBlock is invisible by default
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INSIDE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitualCauldronBlockEntity(pos, state);
    }

    // ---------------------------------------------------------------- right-click

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        boolean lit = state.getValue(LIT);
        int fill = state.getValue(LEVEL);

        // 1. Light it.
        if (!lit && held.is(Items.FLINT_AND_STEEL)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 0.6F);
                held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                player.displayClientMessage(Component.translatable("message.livro_das_sombras.caldeirao.aceso"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 4. Complete: open the book menu (client only; the choice comes back as a packet).
        if (fill == COMPLETE) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openCauldronMenu(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!lit) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.caldeirao.apagado"), true);
            return InteractionResult.CONSUME;
        }

        // 2. The blood price.
        if (fill == 0) {
            if (!BloodPrice.pay(player, BLOOD_PRICE)) {
                player.displayClientMessage(Component.translatable("message.livro_das_sombras.sem_sangue"), true);
                return InteractionResult.CONSUME;
            }
            level.setBlock(pos, state.setValue(LEVEL, 1), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.BLOOD_SQUELCH.get(), SoundSource.BLOCKS, 1.0F, 0.7F);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 0.5F);
            bloodBurst((ServerLevel) level, pos, 25);
            spreadStains((ServerLevel) level, pos);
            player.sendSystemMessage(Component.translatable("message.livro_das_sombras.caldeirao.busca_sacrificio")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            return InteractionResult.CONSUME;
        }

        // Waiting for the sacrifice.
        if (!state.getValue(SACRIFICED)) {
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.caldeirao.busca_sacrificio"), true);
            return InteractionResult.CONSUME;
        }

        // 3. Meat in hand also counts, as if it had been thrown in.
        if (level.getBlockEntity(pos) instanceof RitualCauldronBlockEntity cauldron) {
            if (RitualCauldronBlockEntity.meatValue(held) > 0.0F) {
                ItemStack rest = cauldron.feed(held.copy(), player);
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, rest);
                }
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.translatable("message.livro_das_sombras.caldeirao.mais_carne",
                    cauldron.percentText()), true);
        }
        return InteractionResult.CONSUME;
    }

    // ---------------------------------------------------------------- meat thrown in

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ItemEntity item) || !item.isAlive()) {
            return;
        }
        int fill = state.getValue(LEVEL);
        if (!state.getValue(LIT) || !state.getValue(SACRIFICED) || fill == 0 || fill == COMPLETE) {
            return;
        }
        // Only items that actually sit in the bowl, not on the rim.
        if (entity.getY() > pos.getY() + 0.95D) {
            return;
        }
        ItemStack stack = item.getItem();
        if (RitualCauldronBlockEntity.meatValue(stack) <= 0.0F) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof RitualCauldronBlockEntity cauldron) {
            ItemStack rest = cauldron.feed(stack.copy(), null);
            if (rest.isEmpty()) {
                item.discard();
            } else {
                // Only one piece is taken; the rest is spat back out over the rim.
                item.setItem(rest);
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Math.PI * 2.0D;
                item.setPos(pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D);
                item.setDeltaMovement(Math.cos(angle) * 0.22D, 0.32D, Math.sin(angle) * 0.22D);
            }
        }
    }

    // ---------------------------------------------------------------- sacrifice

    /**
     * A creature killed by a player on top of a waiting cauldron, or within one block of it, is its sacrifice.
     * Called from the death event.
     */
    public static void onSacrifice(ServerLevel level, LivingEntity victim) {
        BlockPos at = victim.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-1, -2, -1), at.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof RitualCauldronBlock)
                    || !state.getValue(LIT) || state.getValue(LEVEL) != 1 || state.getValue(SACRIFICED)) {
                continue;
            }
            BlockPos cauldron = pos.immutable();
            level.setBlock(cauldron, state.setValue(SACRIFICED, true), Block.UPDATE_ALL);
            level.playSound(null, cauldron, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 0.35F, 1.6F);
            level.playSound(null, cauldron, ModSounds.BLOOD_SQUELCH.get(), SoundSource.BLOCKS, 1.0F, 0.6F);
            bloodBurst(level, cauldron, 40);
            level.sendParticles(BLOOD, victim.getX(), victim.getY() + victim.getBbHeight() / 2.0D, victim.getZ(),
                    30, 0.3D, 0.4D, 0.3D, 0.05D);
            Component message = Component.translatable("message.livro_das_sombras.caldeirao.busca_carne")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
            for (Player player : nearbyPlayers(level, cauldron)) {
                player.sendSystemMessage(message);
            }
            return; // one creature feeds one cauldron
        }
    }

    // ---------------------------------------------------------------- blood stains

    /** Blood splashes on the ground around the cauldron: always the 8 blocks next to it, some further out. */
    private static void spreadStains(ServerLevel level, BlockPos center) {
        RandomSource random = level.getRandom();
        BlockState stain = ModBlocks.MANCHA_DE_SANGUE.get().defaultBlockState();
        for (int dx = -STAIN_RADIUS; dx <= STAIN_RADIUS; dx++) {
            for (int dz = -STAIN_RADIUS; dz <= STAIN_RADIUS; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                boolean near = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                if (!near && random.nextInt(3) != 0) {
                    continue;
                }
                // Find the ground at this column, a block above or below the cauldron's floor at most.
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (level.getBlockState(p).isAir() && stain.canSurvive(level, p)) {
                        level.setBlock(p, stain, Block.UPDATE_ALL);
                        break;
                    }
                }
            }
        }
    }

    /** Wipes the stains (when the ritual ends or the cauldron is broken). */
    public static void clearStains(Level level, BlockPos center) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-STAIN_RADIUS, -1, -STAIN_RADIUS),
                center.offset(STAIN_RADIUS, 1, STAIN_RADIUS))) {
            if (level.getBlockState(p).is(ModBlocks.MANCHA_DE_SANGUE.get())) {
                level.removeBlock(p, false);
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            clearStains(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ---------------------------------------------------------------- looks

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            // Small flames under the bowl.
            for (int i = 0; i < 2; i++) {
                level.addParticle(random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME,
                        pos.getX() + 0.2D + random.nextDouble() * 0.6D, pos.getY() + 0.05D,
                        pos.getZ() + 0.2D + random.nextDouble() * 0.6D, 0.0D, 0.01D, 0.0D);
            }
            if (random.nextInt(12) == 0) {
                level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 0.8F, false);
            }
        }
        int fill = state.getValue(LEVEL);
        if (fill > 0 && random.nextInt(3) == 0) {
            // Bubbles on the blood's surface.
            double surface = pos.getY() + RitualCauldronBlockEntity.surfaceHeight(fill) + 0.02D;
            level.addParticle(BLOOD, pos.getX() + 0.2D + random.nextDouble() * 0.6D, surface,
                    pos.getZ() + 0.2D + random.nextDouble() * 0.6D, 0.0D, 0.0D, 0.0D);
        }
        if (fill == COMPLETE && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5D, pos.getY() + 1.6D, pos.getZ() + 0.5D,
                    random.nextGaussian() * 0.5D, -0.6D, random.nextGaussian() * 0.5D);
        }
    }

    public static void bloodBurstPublic(ServerLevel level, BlockPos pos) {
        bloodBurst(level, pos, 50);
    }

    static void bloodBurst(ServerLevel level, BlockPos pos, int count) {
        level.sendParticles(BLOOD, pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                count, 0.25D, 0.1D, 0.25D, 0.02D);
    }

    /** Everyone close to the cauldron. */
    static Iterable<Player> nearbyPlayers(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(16.0D));
    }
}
