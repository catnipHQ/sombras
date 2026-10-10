package com.livrodassombras.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Shared look for the mod's menus: a quiet ritual page.
 * A faint, slowly turning blood circle sits behind a list of numbered options;
 * hovering an option lights it in red. Number keys choose directly.
 */
public abstract class RitualScreen extends Screen {
    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private static final int ROW_HEIGHT = 22;
    private static final int ROW_GAP = 3;

    // Palette: blood on near-black, kept low-contrast on purpose.
    protected static final int TITLE = 0xC23B3B;
    protected static final int SUBTITLE = 0x7A5858;
    private static final int HINT = 0x4E3838;
    private static final int TEXT = 0xB5A3A3;
    private static final int TEXT_HOVER = 0xF2E2E2;
    private static final int TEXT_DISABLED = 0x5A4646;
    private static final int DETAIL = 0x6E5050;
    private static final int DETAIL_HOVER = 0xB86A6A;
    private static final int NUMERAL = 0x5E3838;
    private static final int NUMERAL_HOVER = 0xD84848;
    private static final int RUNE = 0xA01212;

    /** One entry: item icon or texture icon (18x18), name, optional detail on the right. */
    protected record Option(Component name, @Nullable Component detail, ItemStack item,
                            @Nullable ResourceLocation texture, boolean enabled) {}

    protected final List<Option> options = new ArrayList<>();
    private final int rowWidth;
    private int listTop;
    private int listHeight;
    private long openedAt;

    protected RitualScreen(Component title, int rowWidth) {
        super(title);
        this.rowWidth = rowWidth;
    }

    /** Fills {@link #options}. Called on open and whenever {@link #rebuild()} is called. */
    protected abstract void buildOptions();

    /** The line under the title. Called every frame, so it can be live. */
    protected abstract Component subtitle();

    protected abstract void choose(int index);

    @Override
    protected void init() {
        this.openedAt = Util.getMillis();
        this.rebuild();
    }

    protected void rebuild() {
        this.clearWidgets();
        this.options.clear();
        this.buildOptions();
        this.listHeight = this.options.size() * ROW_HEIGHT + (this.options.size() - 1) * ROW_GAP;
        this.listTop = this.height / 2 - this.listHeight / 2 + 14;
        int x = this.width / 2 - this.rowWidth / 2;
        for (int i = 0; i < this.options.size(); i++) {
            this.addRenderableWidget(new RitualRow(i, x, this.listTop + i * (ROW_HEIGHT + ROW_GAP)));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int index = keyCode - GLFW.GLFW_KEY_1;
        if (index >= 0 && index < this.options.size()) {
            if (this.options.get(index).enabled()) {
                this.choose(index);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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
        float radius = Math.min(Math.max(this.listHeight / 2.0F + 30.0F, 70.0F), this.height / 2.0F - 6.0F);
        double turn = (Util.getMillis() % 240000L) / 240000.0D * Math.PI * 2.0D;

        int outer = withAlpha(RUNE, 0.30F * fade);
        int inner = withAlpha(RUNE, 0.16F * fade);
        int star = withAlpha(RUNE, 0.10F * fade);

        dottedRing(graphics, cx, cy, radius, turn, 3, outer);
        dottedRing(graphics, cx, cy, radius - 7.0F, -turn, 4, inner);

        for (int i = 0; i < 8; i++) {
            double a = turn + i * Math.PI / 4.0D;
            int mx = cx + (int) Math.round(Math.cos(a) * (radius - 3.5F));
            int my = cy + (int) Math.round(Math.sin(a) * (radius - 3.5F));
            graphics.fill(mx - 1, my, mx + 2, my + 1, outer);
            graphics.fill(mx, my - 1, mx + 1, my + 2, outer);
        }

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
        int titleWidth = -spacing;
        for (char c : title.toCharArray()) {
            titleWidth += this.font.width(String.valueOf(c)) + spacing;
        }
        int tx = cx - titleWidth / 2;
        int titleY = this.listTop - 46;
        for (char c : title.toCharArray()) {
            String s = String.valueOf(c);
            graphics.drawString(this.font, s, tx, titleY, textColor(TITLE, f), false);
            tx += this.font.width(s) + spacing;
        }

        divider(graphics, cx, titleY + 14, 70, f);

        Component subtitle = this.subtitle().copy().withStyle(ChatFormatting.ITALIC);
        graphics.drawString(this.font, subtitle, cx - this.font.width(subtitle) / 2, titleY + 23, textColor(SUBTITLE, f), false);

        super.render(graphics, mouseX, mouseY, partialTick);

        Component hint = Component.translatable("gui.livro_das_sombras.ritual.hint", this.options.size());
        graphics.drawString(this.font, hint, cx - this.font.width(hint) / 2,
                this.listTop + this.listHeight + 12, textColor(HINT, f), false);
    }

    // ---------------------------------------------------------------- rows

    /** One option: numeral, icon, name and detail. Lights up in red when hovered or focused. */
    private class RitualRow extends AbstractButton {
        private final int index;

        RitualRow(int index, int x, int y) {
            super(x, y, RitualScreen.this.rowWidth, ROW_HEIGHT, RitualScreen.this.options.get(index).name());
            this.index = index;
            this.active = RitualScreen.this.options.get(index).enabled();
        }

        @Override
        public void onPress() {
            RitualScreen.this.choose(this.index);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            float f = RitualScreen.this.fade();
            Option option = RitualScreen.this.options.get(this.index);
            boolean lit = this.active && this.isHoveredOrFocused();
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();

            if (lit) {
                for (int i = 0; i < w; i++) {
                    float a = 0.30F * (1.0F - (float) i / w);
                    graphics.fill(x + i, y, x + i + 1, y + h, withAlpha(0x5A0000, a * f));
                }
                graphics.fill(x, y, x + 1, y + h, withAlpha(0xC81818, f));
            }
            for (int i = 0; i < w; i++) {
                float a = (lit ? 0.45F : 0.18F) * (1.0F - Math.abs(i - w / 2.0F) / (w / 2.0F));
                graphics.fill(x + i, y + h - 1, x + i + 1, y + h, withAlpha(RUNE, a * f));
            }

            Font font = RitualScreen.this.font;
            int textY = y + (h - 8) / 2;

            String numeral = this.index < NUMERALS.length ? NUMERALS[this.index] : String.valueOf(this.index + 1);
            graphics.drawString(font, numeral, x + 18 - font.width(numeral) / 2, textY,
                    textColor(lit ? NUMERAL_HOVER : NUMERAL, f), false);

            int iconX = x + 32;
            int iconY = y + (h - 16) / 2;
            if (option.texture() != null) {
                graphics.blit(option.texture(), iconX, iconY, 16, 16, 0.0F, 0.0F, 18, 18, 18, 18);
            } else {
                graphics.renderItem(option.item(), iconX, iconY);
            }

            int nameColor = !this.active ? TEXT_DISABLED : lit ? TEXT_HOVER : TEXT;
            graphics.drawString(font, this.getMessage(), iconX + 24, textY, textColor(nameColor, f), lit);

            if (option.detail() != null) {
                int dx = x + w - 8 - font.width(option.detail());
                graphics.drawString(font, option.detail(), dx, textY,
                        textColor(!this.active ? TEXT_DISABLED : lit ? DETAIL_HOVER : DETAIL, f), false);
            }
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
