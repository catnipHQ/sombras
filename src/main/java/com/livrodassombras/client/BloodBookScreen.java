package com.livrodassombras.client;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.network.SelectWeaponPacket;
import com.livrodassombras.summon.BloodWeapons;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The menu opened by the Livro do Sangue: a quiet ritual page.
 * A faint, slowly turning blood circle sits behind a list of offerings (the weapons and the Ser de Sangue);
 * hovering an offering lights it in red. Keys 1-5 choose directly.
 */
public class BloodBookScreen extends Screen {
    private static final ResourceLocation BEING_ICON =
            new ResourceLocation(LivroDasSombras.MOD_ID, "textures/mob_effect/ser_de_sangue.png");
    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private static final int ROW_WIDTH = 176;
    private static final int ROW_HEIGHT = 22;
    private static final int ROW_GAP = 3;

    // Palette: blood on near-black, kept low-contrast on purpose.
    private static final int TITLE = 0xC23B3B;
    private static final int SUBTITLE = 0x7A5858;
    private static final int HINT = 0x4E3838;
    private static final int TEXT = 0xB5A3A3;
    private static final int TEXT_HOVER = 0xF2E2E2;
    private static final int NUMERAL = 0x5E3838;
    private static final int NUMERAL_HOVER = 0xD84848;
    private static final int RUNE = 0xA01212;

    private final List<Option> options = new ArrayList<>();
    private int listTop;
    private int listHeight;
    private long openedAt;

    private record Option(Component name, ItemStack icon) {}

    public BloodBookScreen() {
        super(Component.translatable("gui.livro_das_sombras.livro_do_sangue.title"));
        for (Item weapon : BloodWeapons.menuOrder()) {
            this.options.add(new Option(weapon.getDescription(), new ItemStack(weapon)));
        }
        // The Ser de Sangue comes last (index BloodWeapons.beingIndex()).
        this.options.add(new Option(Component.translatable("entity.livro_das_sombras.ser_de_sangue"), ItemStack.EMPTY));
    }

    @Override
    protected void init() {
        this.openedAt = Util.getMillis();
        this.listHeight = this.options.size() * ROW_HEIGHT + (this.options.size() - 1) * ROW_GAP;
        this.listTop = this.height / 2 - this.listHeight / 2 + 14;
        int x = this.width / 2 - ROW_WIDTH / 2;

        for (int i = 0; i < this.options.size(); i++) {
            this.addRenderableWidget(new RitualRow(i, x, this.listTop + i * (ROW_HEIGHT + ROW_GAP)));
        }
    }

    private void choose(int index) {
        ModNetwork.CHANNEL.sendToServer(new SelectWeaponPacket(index));
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int index = keyCode - GLFW.GLFW_KEY_1;
        if (index >= 0 && index < this.options.size()) {
            this.choose(index);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---------------------------------------------------------------- drawing

    private float fade() {
        return Mth.clamp((Util.getMillis() - this.openedAt) / 350.0F, 0.0F, 1.0F);
    }

    private static int withAlpha(int rgb, float alpha) {
        return (Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** Text colors: the font draws near-zero alpha as fully opaque, so never go below a small floor. */
    private static int textColor(int rgb, float alpha) {
        return withAlpha(rgb, Math.max(alpha, 0.06F));
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        float f = this.fade();
        // Dark, slightly red-tinted veil; heavier at the bottom.
        graphics.fillGradient(0, 0, this.width, this.height, withAlpha(0x0A0000, 0.80F * f), withAlpha(0x000000, 0.93F * f));
        this.renderCircle(graphics, f);
    }

    /** The blood circle: two dotted rings, a pentagram and eight small marks, turning very slowly. */
    private void renderCircle(GuiGraphics graphics, float fade) {
        int cx = this.width / 2;
        int cy = this.listTop + this.listHeight / 2;
        float radius = Math.min(this.listHeight / 2.0F + 30.0F, this.height / 2.0F - 6.0F);
        double turn = (Util.getMillis() % 240000L) / 240000.0D * Math.PI * 2.0D;

        int outer = withAlpha(RUNE, 0.30F * fade);
        int inner = withAlpha(RUNE, 0.16F * fade);
        int star = withAlpha(RUNE, 0.10F * fade);

        dottedRing(graphics, cx, cy, radius, turn, 3, outer);
        dottedRing(graphics, cx, cy, radius - 7.0F, -turn, 4, inner);

        // Marks between the rings.
        for (int i = 0; i < 8; i++) {
            double a = turn + i * Math.PI / 4.0D;
            int mx = cx + (int) Math.round(Math.cos(a) * (radius - 3.5F));
            int my = cy + (int) Math.round(Math.sin(a) * (radius - 3.5F));
            graphics.fill(mx - 1, my, mx + 2, my + 1, outer);
            graphics.fill(mx, my - 1, mx + 1, my + 2, outer);
        }

        // Pentagram inscribed in the inner ring.
        float r = radius - 9.0F;
        for (int i = 0; i < 5; i++) {
            double a1 = -turn - Math.PI / 2.0D + i * Math.PI * 2.0D / 5.0D;
            double a2 = a1 + Math.PI * 4.0D / 5.0D;
            line(graphics, cx + Math.cos(a1) * r, cy + Math.sin(a1) * r, cx + Math.cos(a2) * r, cy + Math.sin(a2) * r, star);
        }
    }

    private static void dottedRing(GuiGraphics graphics, int cx, int cy, float radius, double offset, int spacing, int color) {
        int dots = Math.max(12, (int) (Math.PI * 2.0D * radius / spacing));
        for (int i = 0; i < dots; i++) {
            double a = offset + i * Math.PI * 2.0D / dots;
            int x = cx + (int) Math.round(Math.cos(a) * radius);
            int y = cy + (int) Math.round(Math.sin(a) * radius);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void line(GuiGraphics graphics, double x1, double y1, double x2, double y2, int color) {
        int steps = (int) Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)) / 2;
        for (int s = 0; s <= steps; s++) {
            double t = steps == 0 ? 0 : (double) s / steps;
            int x = (int) Math.round(Mth.lerp(t, x1, x2));
            int y = (int) Math.round(Mth.lerp(t, y1, y2));
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    /** A hairline that fades out towards both ends, with a small diamond in the middle. */
    private static void divider(GuiGraphics graphics, int cx, int y, int halfWidth, float fade) {
        for (int i = 0; i < halfWidth; i++) {
            int c = withAlpha(RUNE, (1.0F - (float) i / halfWidth) * 0.55F * fade);
            graphics.fill(cx + 4 + i, y, cx + 5 + i, y + 1, c);
            graphics.fill(cx - 5 - i, y, cx - 4 - i, y + 1, c);
        }
        int d = withAlpha(TITLE, fade);
        graphics.fill(cx - 1, y - 1, cx + 2, y + 2, d);
        graphics.fill(cx, y - 2, cx + 1, y + 3, d);
        graphics.fill(cx - 2, y, cx + 3, y + 1, d);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        float f = this.fade();
        int cx = this.width / 2;

        // Title in widely spaced capitals.
        String title = this.title.getString().toUpperCase();
        int spacing = 2;
        int titleWidth = 0;
        for (char c : title.toCharArray()) {
            titleWidth += this.font.width(String.valueOf(c)) + spacing;
        }
        titleWidth -= spacing;
        int tx = cx - titleWidth / 2;
        int titleY = this.listTop - 46;
        for (char c : title.toCharArray()) {
            String s = String.valueOf(c);
            graphics.drawString(this.font, s, tx, titleY, textColor(TITLE, f), false);
            tx += this.font.width(s) + spacing;
        }

        divider(graphics, cx, titleY + 14, 70, f);

        Component cost = Component.translatable("gui.livro_das_sombras.livro_do_sangue.cost").withStyle(ChatFormatting.ITALIC);
        graphics.drawString(this.font, cost, cx - this.font.width(cost) / 2, titleY + 23, textColor(SUBTITLE, f), false);

        super.render(graphics, mouseX, mouseY, partialTick);

        Component hint = Component.translatable("gui.livro_das_sombras.livro_do_sangue.hint", this.options.size());
        graphics.drawString(this.font, hint, cx - this.font.width(hint) / 2,
                this.listTop + this.listHeight + 12, textColor(HINT, f), false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- rows

    /** One offering: numeral, icon and name. Lights up in red when hovered or focused. */
    private class RitualRow extends AbstractButton {
        private final int index;

        RitualRow(int index, int x, int y) {
            super(x, y, ROW_WIDTH, ROW_HEIGHT, BloodBookScreen.this.options.get(index).name());
            this.index = index;
        }

        @Override
        public void onPress() {
            BloodBookScreen.this.choose(this.index);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            float f = BloodBookScreen.this.fade();
            boolean lit = this.isHoveredOrFocused();
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();

            if (lit) {
                // Soft red wash from the left, and a thin bright edge.
                for (int i = 0; i < w; i++) {
                    float a = 0.30F * (1.0F - (float) i / w);
                    graphics.fill(x + i, y, x + i + 1, y + h, withAlpha(0x5A0000, a * f));
                }
                graphics.fill(x, y, x + 1, y + h, withAlpha(0xC81818, f));
            }
            // Hairline under every row.
            for (int i = 0; i < w; i++) {
                float a = (lit ? 0.45F : 0.18F) * (1.0F - Math.abs(i - w / 2.0F) / (w / 2.0F));
                graphics.fill(x + i, y + h - 1, x + i + 1, y + h, withAlpha(RUNE, a * f));
            }

            var font = BloodBookScreen.this.font;
            int textY = y + (h - 8) / 2;

            String numeral = this.index < NUMERALS.length ? NUMERALS[this.index] : String.valueOf(this.index + 1);
            graphics.drawString(font, numeral, x + 18 - font.width(numeral) / 2, textY,
                    textColor(lit ? NUMERAL_HOVER : NUMERAL, f), false);

            int iconX = x + 32;
            int iconY = y + (h - 16) / 2;
            Option option = BloodBookScreen.this.options.get(this.index);
            if (option.icon().isEmpty()) {
                graphics.blit(BEING_ICON, iconX, iconY, 16, 16, 0.0F, 0.0F, 18, 18, 18, 18);
            } else {
                graphics.renderItem(option.icon(), iconX, iconY);
            }

            graphics.drawString(font, this.getMessage(), iconX + 24, textY, textColor(lit ? TEXT_HOVER : TEXT, f), lit);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
