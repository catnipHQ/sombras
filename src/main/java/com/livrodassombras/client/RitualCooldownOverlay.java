package com.livrodassombras.client;

import com.livrodassombras.ritual.ClientRitualCooldowns;
import com.livrodassombras.ritual.MinorRitual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Bottom-left corner of the HUD: one small red bar per ritual that is recharging, with the ritual's icon. */
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
            if (left <= 0.0F) {
                continue;
            }
            int x = MARGIN;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y + 1, 0.0F);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.renderItem(ritual.icon(), 0, 0);
            graphics.pose().popPose();

            int bx = x + 11;
            int by = y + 4;
            graphics.fill(bx - 1, by - 1, bx + BAR_WIDTH + 1, by + 3, 0x99000000);
            graphics.fill(bx, by, bx + BAR_WIDTH, by + 2, 0xFF2A0000);
            graphics.fill(bx, by, bx + Math.max(1, (int) (BAR_WIDTH * left)), by + 2, 0xFFC41E1E);
            y -= ROW;
        }
    }
}
