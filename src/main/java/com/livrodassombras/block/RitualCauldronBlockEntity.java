package com.livrodassombras.block;

import com.livrodassombras.registry.ModBlockEntities;
import com.livrodassombras.registry.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Remembers how much meat the Caldeirão Ritualístico has taken.
 * <p>
 * Each piece of meat adds 1 + its saturation (one piece at a time); the cauldron is complete at {@link #REQUIRED}.
 * So it takes about 34 rotten flesh, 22 raw beef, 8 cooked chicken or 5 steaks.
 */
public class RitualCauldronBlockEntity extends BlockEntity {
    public static final float REQUIRED = 60.0F;

    private float meat;

    public RitualCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CALDEIRAO_RITUALISTICO.get(), pos, state);
    }

    /** How much one piece of this item fills the cauldron, or 0 if it isn't meat. */
    public static float meatValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0F;
        }
        FoodProperties food = stack.getFoodProperties(null);
        if (food == null || !food.isMeat()) {
            return 0.0F;
        }
        float saturation = food.getNutrition() * food.getSaturationModifier() * 2.0F;
        return 1.0F + saturation;
    }

    /** Height of the liquid's surface for a fill level, in blocks from the bottom (matches the models). */
    public static double surfaceHeight(int level) {
        return switch (level) {
            case 1 -> 6.0D / 16.0D;
            case 2 -> 9.0D / 16.0D;
            case 3 -> 12.0D / 16.0D;
            default -> 15.0D / 16.0D;
        };
    }

    public Component percentText() {
        int percent = Math.min(100, (int) Math.floor(this.meat / REQUIRED * 100.0F));
        return Component.literal(percent + "%");
    }

    /**
     * Takes exactly one piece of meat from {@code stack}.
     *
     * @param feeder the player who put it in by hand, or null for thrown meat
     * @return what is left of the stack
     */
    public ItemStack feed(ItemStack stack, @Nullable Player feeder) {
        if (!(this.level instanceof ServerLevel level)) {
            return stack;
        }
        BlockState state = this.getBlockState();
        if (!state.hasProperty(RitualCauldronBlock.LEVEL)) {
            return stack;
        }
        int fill = state.getValue(RitualCauldronBlock.LEVEL);
        if (fill == 0 || fill == RitualCauldronBlock.COMPLETE || !state.getValue(RitualCauldronBlock.SACRIFICED)) {
            return stack;
        }

        float value = meatValue(stack);
        if (value <= 0.0F) {
            return stack;
        }
        stack.shrink(1);
        this.meat += value;
        int eaten = 1;
        this.setChanged();

        BlockPos pos = this.getBlockPos();
        level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.8F, 0.6F);
        level.playSound(null, pos, ModSounds.BLOOD_SQUELCH.get(), SoundSource.BLOCKS, 0.7F, 0.8F);
        RitualCauldronBlock.bloodBurst(level, pos, 8 + eaten * 2);

        int newFill;
        if (this.meat >= REQUIRED) {
            newFill = RitualCauldronBlock.COMPLETE;
        } else if (this.meat >= REQUIRED * 2.0F / 3.0F) {
            newFill = 3;
        } else if (this.meat >= REQUIRED / 3.0F) {
            newFill = 2;
        } else {
            newFill = 1;
        }
        if (newFill != fill) {
            level.setBlock(pos, state.setValue(RitualCauldronBlock.LEVEL, newFill), Block.UPDATE_ALL);
        }

        if (newFill == RitualCauldronBlock.COMPLETE) {
            level.playSound(null, pos, ModSounds.BLOOD_SUMMON.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
            RitualCauldronBlock.bloodBurst(level, pos, 60);
            Component done = Component.translatable("message.livro_das_sombras.caldeirao.completo")
                    .withStyle(ChatFormatting.DARK_RED);
            for (Player player : RitualCauldronBlock.nearbyPlayers(level, pos)) {
                player.sendSystemMessage(done);
            }
        } else {
            Component progress = Component.translatable("message.livro_das_sombras.caldeirao.progresso", this.percentText());
            if (feeder != null) {
                feeder.displayClientMessage(progress, true);
            } else {
                for (Player player : RitualCauldronBlock.nearbyPlayers(level, pos)) {
                    player.displayClientMessage(progress, true);
                }
            }
        }
        return stack;
    }

    /** Back to the start: empty and unlit. */
    public void reset() {
        this.meat = 0.0F;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("Meat", this.meat);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.meat = tag.getFloat("Meat");
    }
}
