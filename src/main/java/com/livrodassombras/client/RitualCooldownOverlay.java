package com.livrodassombras.client;

import com.livrodassombras.ritual.BookPath;
import com.livrodassombras.ritual.ClientRevertMark;
import com.livrodassombras.ritual.ClientRitualCooldowns;
import com.livrodassombras.ritual.MinorRitual;
import com.livrodassombras.ritual.RevertMarks;
import com.livrodassombras.summon.TimeSummon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Bottom-left corner of the HUD: one small bar per ritual (or summon) that is recharging, with its icon,
 * red for the Blood path and grey for the Time path. Above them, the Reverter Passos mark: distance and time left.
 */
public class RitualCooldownOverlay implements IGuiOverlay {
    private static final int MARGIN = 4;
    private static final int ROW = 10;
    private static final int BAR_WIDTH = 34;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        long now = mc.level.getGameTime();
        int y = height - MARGIN - ROW;
        for (MinorRitual ritual : MinorRitual.values()) {
            float left = ClientRitualCooldowns.fraction(ritual, now);
            if (left > 0.0F) {
                bar(graphics, y, ritual.icon(), left, ritual.path() == BookPath.TIME);
                y -= ROW;
            }
        }
        for (TimeSummon summon : TimeSummon.values()) {
            float left = ClientRitualCooldowns.fraction(summon.id(), summon.cooldownTicks(), now);
            if (left > 0.0F) {
                bar(graphics, y, summon.icon(), left, true);
                y -= ROW;
            }
        }

        // Reverter Passos: "Passo 12/25 · 4:12", turning red near the edge.
        Vec3 spot = ClientRevertMark.spot();
        if (spot != null) {
            double distance = mc.player.position().distanceTo(spot);
            long ticks = Math.max(0L, ClientRevertMark.ends() - now);
            Component text = Component.translatable("hud.livro_das_sombras.passo", (int) distance, (int) RevertMarks.RANGE,
                    ticks / 1200L, String.format("%02d", ticks / 20L % 60L));
            int color = distance > RevertMarks.WARN_RANGE ? 0xFFE05050 : 0xFFC8C8C8;
            graphics.fill(MARGIN - 1, y + 1, MARGIN + mc.font.width(text) + 2, y + 10, 0x99000000);
            graphics.drawString(mc.font, text, MARGIN + 1, y + 2, color, false);
        }
    }

    private static void bar(GuiGraphics graphics, int y, ItemStack icon, float left, boolean time) {
        int x = MARGIN;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + 1, 0.0F);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.renderItem(icon, 0, 0);
        graphics.pose().popPose();

        int bx = x + 11;
        int by = y + 4;
        graphics.fill(bx - 1, by - 1, bx + BAR_WIDTH + 1, by + 3, 0x99000000);
        graphics.fill(bx, by, bx + BAR_WIDTH, by + 2, time ? 0xFF1E1E1E : 0xFF2A0000);
        graphics.fill(bx, by, bx + Math.max(1, (int) (BAR_WIDTH * left)), by + 2, time ? 0xFFBEBEBE : 0xFFC41E1E);
    }
}
