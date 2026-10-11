package com.livrodassombras.client;

import com.livrodassombras.entity.SoulGolemEntity;
import com.livrodassombras.network.GolemFormPacket;
import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.summon.SpiritBond;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Golem de Almas' menu (Shift + right-click, summoner only), in the Livro dos Espíritos' light blue:
 * I. Colosso, II. Forma Destruidora (choosing the active form again goes back to normal), III. Morra.
 * The server checks everything again; a disabled option just shows what can't be done right now.
 */
public class SoulGolemScreen extends RitualScreen {
    private final int entityId;
    private SoulGolemEntity.Form lastForm;
    private boolean lastCanDestroy;

    public SoulGolemScreen(int entityId) {
        super(Component.translatable("entity.livro_das_sombras.golem_de_almas"), 236, Theme.SPIRIT);
        this.entityId = entityId;
    }

    private SoulGolemEntity golem() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getEntity(this.entityId) instanceof SoulGolemEntity golem ? golem : null;
    }

    private static boolean canDestroy(Player player, SoulGolemEntity golem) {
        return golem.getForm() == SoulGolemEntity.Form.DESTROYER || player.getAbilities().instabuild
                || SpiritBond.canSacrifice(player);
    }

    @Override
    protected void buildOptions() {
        Player player = Minecraft.getInstance().player;
        SoulGolemEntity golem = this.golem();
        if (player == null || golem == null) {
            return;
        }
        this.lastForm = golem.getForm();
        this.lastCanDestroy = canDestroy(player, golem);
        String key = "gui.livro_das_sombras.golem_de_almas.";

        boolean colossus = this.lastForm == SoulGolemEntity.Form.COLOSSUS;
        this.options.add(new Option(Component.translatable(key + "colosso"),
                Component.translatable(key + (colossus ? "ativa" : "colosso.detail")),
                new ItemStack(Items.ANVIL), null, true));

        boolean destroyer = this.lastForm == SoulGolemEntity.Form.DESTROYER;
        this.options.add(new Option(Component.translatable(key + "destruidora"),
                Component.translatable(key + (destroyer ? "destruidora.ativa" : "destruidora.detail")),
                new ItemStack(Items.SOUL_CAMPFIRE), null, this.lastCanDestroy));

        this.options.add(new Option(Component.translatable(key + "morra"),
                Component.translatable(key + "morra.detail"),
                new ItemStack(Items.WITHER_ROSE), null, true));
    }

    @Override
    public void tick() {
        super.tick();
        Player player = Minecraft.getInstance().player;
        SoulGolemEntity golem = this.golem();
        if (player == null || golem == null || !golem.isAlive() || player.distanceToSqr(golem) > 10.0D * 10.0D) {
            this.onClose(); // the golem is gone or too far away
            return;
        }
        if (golem.getForm() != this.lastForm || canDestroy(player, golem) != this.lastCanDestroy) {
            this.rebuild();
        }
    }

    @Override
    protected Component subtitle() {
        SoulGolemEntity golem = this.golem();
        if (golem == null) {
            return Component.empty();
        }
        return Component.translatable("gui.livro_das_sombras.golem_de_almas.subtitle",
                (int) Math.ceil(golem.getHealth()), (int) golem.getMaxHealth(),
                Component.translatable("gui.livro_das_sombras.golem_de_almas.forma." + golem.getForm().name().toLowerCase()));
    }

    @Override
    protected void choose(int index) {
        ModNetwork.CHANNEL.sendToServer(new GolemFormPacket(this.entityId, index));
        this.onClose();
    }
}
