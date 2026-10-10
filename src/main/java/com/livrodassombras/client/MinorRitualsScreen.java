package com.livrodassombras.client;

import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.network.SelectRitualPacket;
import com.livrodassombras.registry.ModItems;
import com.livrodassombras.ritual.MinorRitual;
import com.livrodassombras.ritual.RitualBook;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Rituais menores: choosing one charges the book; it's cast later with right-click. */
public class MinorRitualsScreen extends RitualScreen {
    private long lastSecond = -1L;

    public MinorRitualsScreen() {
        super(Component.translatable("gui.livro_das_sombras.rituais.title"), 268);
    }

    private static ItemStack heldBook() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.LIVRO_DO_SANGUE.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0L : mc.level.getGameTime();
    }

    @Override
    protected void buildOptions() {
        ItemStack book = heldBook();
        long now = gameTime();
        for (MinorRitual ritual : MinorRitual.values()) {
            long left = book.isEmpty() ? 0L : RitualBook.cooldownLeft(book, ritual, now);
            Component detail = left > 0L
                    ? Component.translatable("gui.livro_das_sombras.rituais.recarga", RitualBook.seconds(left))
                    : ritual.detail();
            this.options.add(new Option(ritual.displayName(), detail, ritual.icon(), null, true));
        }
    }

    @Override
    protected Component subtitle() {
        MinorRitual charged = RitualBook.charged(heldBook());
        return charged == null
                ? Component.translatable("gui.livro_das_sombras.rituais.subtitle")
                : Component.translatable("gui.livro_das_sombras.rituais.preparado", charged.displayName());
    }

    @Override
    public void tick() {
        super.tick();
        // Keep the cooldown counters current.
        long second = gameTime() / 20L;
        if (second != this.lastSecond) {
            this.lastSecond = second;
            this.rebuild();
        }
    }

    @Override
    protected void choose(int index) {
        ModNetwork.CHANNEL.sendToServer(new SelectRitualPacket(index));
        this.onClose();
    }
}
