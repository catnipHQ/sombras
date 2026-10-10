package com.livrodassombras.client;

import com.livrodassombras.block.RitualCauldronBlock;
import com.livrodassombras.network.CauldronChoicePacket;
import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.registry.ModBlocks;
import com.livrodassombras.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** The full Caldeirão Ritualístico's menu: I. Livro do Sangue, II. Livro das Sombras. */
public class RitualCauldronScreen extends RitualScreen {
    private final BlockPos pos;

    public RitualCauldronScreen(BlockPos pos) {
        super(Component.translatable("block.livro_das_sombras.caldeirao_ritualistico"), 220);
        this.pos = pos;
    }

    @Override
    protected void buildOptions() {
        this.options.add(new Option(Component.translatable("item.livro_das_sombras.livro_do_sangue"), null,
                new ItemStack(ModItems.LIVRO_DO_SANGUE.get()), null, true));
        this.options.add(new Option(Component.translatable("item.livro_das_sombras.livro_das_sombras"), null,
                new ItemStack(ModItems.LIVRO_DAS_SOMBRAS.get()), null, true));
    }

    @Override
    protected Component subtitle() {
        return Component.translatable("gui.livro_das_sombras.caldeirao.subtitle");
    }

    @Override
    protected void choose(int index) {
        ModNetwork.CHANNEL.sendToServer(new CauldronChoicePacket(this.pos, index));
        this.onClose();
    }

    @Override
    public void tick() {
        super.tick();
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || player.distanceToSqr(this.pos.getCenter()) > 8.0D * 8.0D) {
            this.onClose();
            return;
        }
        BlockState state = mc.level.getBlockState(this.pos);
        if (!state.is(ModBlocks.CALDEIRAO_RITUALISTICO.get())
                || state.getValue(RitualCauldronBlock.LEVEL) != RitualCauldronBlock.COMPLETE) {
            this.onClose(); // someone else used it, or it was broken
        }
    }
}
