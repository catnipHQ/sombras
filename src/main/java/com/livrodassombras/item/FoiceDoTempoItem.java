package com.livrodassombras.item;

import com.livrodassombras.ritual.RitualCooldowns;
import com.livrodassombras.ritual.TimeRituals;
import com.livrodassombras.summon.TimeSummon;
import com.livrodassombras.summon.TimeSummoning;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Foice do Tempo: hits like a diamond hoe (1 damage, attack speed 4) and tills like one.
 * Right-click on crops pushes time forward on every crop in a 3x3 area (one block up or down too):
 * each plant has a 50% chance to grow up at once and 50% to wither away. Then the scythe vanishes,
 * and this summon can't be used again for 2 minutes.
 */
public class FoiceDoTempoItem extends HoeItem {
    public static final float GROW_CHANCE = 0.5F;

    public FoiceDoTempoItem(Properties properties) {
        super(Tiers.DIAMOND, -3, 0.0F, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        List<BlockPos> crops = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(clicked.offset(-1, -1, -1), clicked.offset(1, 1, 1))) {
            if (ageProperty(level.getBlockState(pos)) != null) {
                crops.add(pos.immutable());
            }
        }
        if (crops.isEmpty()) {
            return super.useOn(context); // no crops: an ordinary hoe
        }
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel serverLevel) || player == null) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        int grown = 0;
        int withered = 0;
        for (BlockPos pos : crops) {
            BlockState state = level.getBlockState(pos);
            IntegerProperty age = ageProperty(state);
            if (age == null) {
                continue;
            }
            if (serverLevel.random.nextFloat() < GROW_CHANCE) {
                int max = age.getPossibleValues().stream().max(Integer::compare).orElse(0);
                serverLevel.setBlock(pos, state.setValue(age, max), Block.UPDATE_ALL);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        6, 0.3D, 0.3D, 0.3D, 0.0D);
                grown++;
            } else {
                serverLevel.destroyBlock(pos, false);
                serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D,
                        8, 0.25D, 0.2D, 0.25D, 0.01D);
                serverLevel.sendParticles(TimeRituals.DARK, pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D,
                        5, 0.25D, 0.2D, 0.25D, 0.0D);
                withered++;
            }
        }

        serverLevel.playSound(null, clicked, SoundEvents.CROP_PLANTED, SoundSource.PLAYERS, 1.0F, 0.6F);
        serverLevel.playSound(null, clicked, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.8F, 0.5F);
        player.displayClientMessage(Component.translatable("message.livro_das_sombras.foice.resultado", grown, withered), true);

        // The scythe is spent: it vanishes, and its summon waits 2 minutes (not in creative).
        context.getItemInHand().shrink(1);
        TimeSummoning.vanish(serverLevel, player.getX(), player.getY() + 1.0D, player.getZ());
        if (player instanceof ServerPlayer serverPlayer && !player.getAbilities().instabuild) {
            RitualCooldowns.start(serverPlayer, TimeSummon.FOICE.id(), TimeSummon.FOICE.cooldownTicks());
        }
        return InteractionResult.CONSUME;
    }

    /** The age property of a crop that can still grow, or null for anything else. */
    @Nullable
    private static IntegerProperty ageProperty(BlockState state) {
        Block block = state.getBlock();
        if (!(block instanceof CropBlock || block instanceof StemBlock || block instanceof SweetBerryBushBlock
                || block instanceof CocoaBlock || block instanceof NetherWartBlock)) {
            return null;
        }
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && property.getName().equals("age")) {
                int max = age.getPossibleValues().stream().max(Integer::compare).orElse(0);
                return state.getValue(age) < max ? age : null;
            }
        }
        return null;
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.livro_das_sombras.foice").withStyle(ChatFormatting.GRAY));
    }
}
