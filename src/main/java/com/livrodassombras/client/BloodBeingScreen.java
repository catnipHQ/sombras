package com.livrodassombras.client;

import com.livrodassombras.entity.BloodBeingEntity;
import com.livrodassombras.network.BeingRitualPacket;
import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.summon.BloodBond;
import com.livrodassombras.summon.BloodPrice;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Ser de Sangue's ritual menu (Shift + right-click, summoner only):
 * I. Prolong: 9.5 hearts for 10 minutes. II. Undo: dies at once. III. Sacrifice: 3 hearts for strength.
 * The server checks everything again; disabled options just show what can't be done right now.
 */
public class BloodBeingScreen extends RitualScreen {
    private final int entityId;
    private boolean lastProlong;
    private boolean lastSacrifice;

    public BloodBeingScreen(int entityId) {
        super(Component.translatable("gui.livro_das_sombras.ser_de_sangue.title"), 236);
        this.entityId = entityId;
    }

    private BloodBeingEntity being() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getEntity(this.entityId) instanceof BloodBeingEntity being ? being : null;
    }

    private boolean canProlong(Player player) {
        return BloodPrice.canPay(player, BloodBeingEntity.PROLONG_COST);
    }

    private boolean canSacrifice(Player player, BloodBeingEntity being) {
        return !being.isSacrificed() && (player.getAbilities().instabuild || BloodBond.canSacrifice(player));
    }

    @Override
    protected void buildOptions() {
        Player player = Minecraft.getInstance().player;
        BloodBeingEntity being = this.being();
        if (player == null || being == null) {
            return;
        }
        this.lastProlong = this.canProlong(player);
        this.lastSacrifice = this.canSacrifice(player, being);

        this.options.add(new Option(Component.translatable("gui.livro_das_sombras.ser_de_sangue.prolong"),
                Component.translatable("gui.livro_das_sombras.ser_de_sangue.prolong.detail"),
                new ItemStack(Items.CLOCK), null, this.lastProlong));
        this.options.add(new Option(Component.translatable("gui.livro_das_sombras.ser_de_sangue.undo"),
                Component.translatable("gui.livro_das_sombras.ser_de_sangue.undo.detail"),
                new ItemStack(Items.WITHER_ROSE), null, true));
        this.options.add(new Option(Component.translatable("gui.livro_das_sombras.ser_de_sangue.sacrifice"),
                being.isSacrificed()
                        ? Component.translatable("gui.livro_das_sombras.ser_de_sangue.sacrifice.done")
                        : Component.translatable("gui.livro_das_sombras.ser_de_sangue.sacrifice.detail"),
                new ItemStack(Items.REDSTONE), null, this.lastSacrifice));
    }

    @Override
    public void tick() {
        super.tick();
        Player player = Minecraft.getInstance().player;
        BloodBeingEntity being = this.being();
        if (player == null || being == null || !being.isAlive() || player.distanceToSqr(being) > 10.0D * 10.0D) {
            this.onClose(); // the creature is gone or too far away
            return;
        }
        // Health changed (or the sacrifice went through): refresh which options are available.
        if (this.canProlong(player) != this.lastProlong || this.canSacrifice(player, being) != this.lastSacrifice) {
            this.rebuild();
        }
    }

    @Override
    protected Component subtitle() {
        BloodBeingEntity being = this.being();
        int seconds = being == null ? 0 : Math.max(0, being.getLifeTicks() / 20);
        return Component.translatable("gui.livro_das_sombras.ser_de_sangue.remaining",
                String.format("%d:%02d", seconds / 60, seconds % 60));
    }

    @Override
    protected void choose(int index) {
        ModNetwork.CHANNEL.sendToServer(new BeingRitualPacket(this.entityId, index));
        this.onClose();
    }
}
