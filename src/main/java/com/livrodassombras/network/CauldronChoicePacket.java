package com.livrodassombras.network;

import com.livrodassombras.block.RitualCauldronBlock;
import com.livrodassombras.block.RitualCauldronBlockEntity;
import com.livrodassombras.registry.ModBlocks;
import com.livrodassombras.registry.ModItems;
import com.livrodassombras.registry.ModSounds;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: the book chosen in the Caldeirão Ritualístico menu. */
public record CauldronChoicePacket(BlockPos pos, int choice) {
    public static final int LIVRO_DO_SANGUE = 0;
    public static final int LIVRO_DAS_SOMBRAS = 1;

    private static final double MAX_DISTANCE_SQR = 8.0D * 8.0D;

    public static void encode(CauldronChoicePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarInt(packet.choice);
    }

    public static CauldronChoicePacket decode(FriendlyByteBuf buf) {
        return new CauldronChoicePacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(CauldronChoicePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            BlockPos pos = packet.pos();
            // Never trust the client: a loaded, complete cauldron, close by.
            if (!level.isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) {
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.CALDEIRAO_RITUALISTICO.get())
                    || state.getValue(RitualCauldronBlock.LEVEL) != RitualCauldronBlock.COMPLETE) {
                return;
            }
            Item book = switch (packet.choice()) {
                case LIVRO_DO_SANGUE -> ModItems.LIVRO_DO_SANGUE.get();
                case LIVRO_DAS_SOMBRAS -> ModItems.LIVRO_DAS_SOMBRAS.get();
                default -> null;
            };
            if (book == null) {
                return;
            }

            // The ritual is spent: the cauldron empties and goes out.
            level.setBlock(pos, state.setValue(RitualCauldronBlock.LEVEL, 0).setValue(RitualCauldronBlock.LIT, false),
                    Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof RitualCauldronBlockEntity cauldron) {
                cauldron.reset();
            }

            ItemStack stack = new ItemStack(book);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            level.playSound(null, pos, ModSounds.BLOOD_SUMMON.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
            RitualCauldronBlock.bloodBurstPublic(level, pos);
        });
        context.setPacketHandled(true);
    }
}
