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

    /**
     * A menu's palette. Blood: red on near-black. Time: white and grey on black, with a clock face
     * instead of the pentagram. Both kept low-contrast on purpose.
     */
    public record Theme(int title, int subtitle, int hint, int text, int textHover, int textDisabled,
                        int detail, int detailHover, int numeral, int numeralHover, int rune,
                        int veil, int rowGlow, int rowEdge, int barBack, int barFill, boolean clock) {
        public static final Theme BLOOD = new Theme(0xC23B3B, 0x7A5858, 0x4E3838, 0xB5A3A3, 0xF2E2E2, 0x5A4646,
                0x6E5050, 0xB86A6A, 0x5E3838, 0xD84848, 0xA01212, 0x0A0000, 0x5A0000, 0xC81818, 0x2A0000, 0xC41E1E, false);
        public static final Theme TIME = new Theme(0xE6E6E6, 0x8C8C8C, 0x4C4C4C, 0xB4B4B4, 0xFFFFFF, 0x4A4A4A,
                0x6E6E6E, 0xC8C8C8, 0x585858, 0xF0F0F0, 0x9E9E9E, 0x030303, 0x3C3C3C, 0xE8E8E8, 0x1E1E1E, 0xBEBEBE, true);
    }

    protected static final int TITLE = 0xC23B3B;
    protected static final int SUBTITLE = 0x7A5858;

    /**
     * One entry: item icon or texture icon (18x18), name, optional detail on the right.
     * {@code cooldown} (0 to 1) replaces the detail with a small red bar while something is recharging.
     */
    protected record Option(Component name, @Nullable Component detail, ItemStack item,
                            @Nullable ResourceLocation texture, boolean enabled, float cooldown) {
        public Option(Component name, @Nullable Component detail, ItemStack item,
                      @Nullable ResourceLocation texture, boolean enabled) {
            this(name, detail, item, texture, enabled, 0.0F);
        }
    }

    private static final int COOLDOWN_BAR_WIDTH = 40;

    protected final List<Option> options = new ArrayList<>();
    private final int minRowWidth;
    private int rowWidth;
    private int backY;
    private int listTop;
    private int listHeight;
    private long openedAt;

    protected final Theme theme;

    protected RitualScreen(Component title, int rowWidth) {
        this(title, rowWidth, Theme.BLOOD);
    }

    protected RitualScreen(Component title, int rowWidth, Theme theme) {
        super(title);
        this.minRowWidth = rowWidth;
        this.rowWidth = rowWidth;
        this.theme = theme;
    }

    /** Fills {@link #options}. Called on open and whenever {@link #rebuild()} is called. */
    protected abstract void buildOptions();

    /** The line under the title. Called every frame, so it can be live. */
    protected abstract Component subtitle();

    protected abstract void choose(int index);

    /** A "Back" link under the list when non-null (Backspace does the same). */
    @Nullable
    protected Component backLabel() {
        return null;
    }

    protected void back() {}

    /** The line at the bottom. */
    protected Component hint() {
        return Component.translatable("gui.livro_das_sombras.ritual.hint", this.options.size());
    }

    @Override
    protected void init() {
        this.openedAt = Util.getMillis();
        this.rebuild();
    }

    protected void rebuild() {
        this.clearWidgets();
        this.options.clear();
        this.buildOptions();
        // Wide enough that a name never runs into its detail.
        int content = 0;
        for (Option option : this.options) {
            int right = option.cooldown() > 0.0F ? COOLDOWN_BAR_WIDTH
                    : option.detail() == null ? 0 : this.font.width(option.detail());
            content = Math.max(content, this.font.width(option.name()) + (right > 0 ? 16 + right : 0));
        }
        this.rowWidth = Math.min(Math.max(this.minRowWidth, 56 + content + 10), this.width - 16);
        this.listHeight = this.options.size() * ROW_HEIGHT + (this.options.size() - 1) * ROW_GAP;
        this.listTop = this.height / 2 - this.listHeight / 2 + 14;
        int x = this.width / 2 - this.rowWidth / 2;
        for (int i = 0; i < this.options.size(); i++) {
            this.addRenderableWidget(new RitualRow(i, x, this.listTop + i * (ROW_HEIGHT + ROW_GAP)));
        }
        this.backY = this.listTop + this.listHeight + 10;
        Component back = this.backLabel();
        if (back != null) {
            Component label = Component.literal("‹ ").append(back);
            int w = this.font.width(label) + 12;
            this.addRenderableWidget(new BackLink(this.width / 2 - w / 2, this.backY - 2, w, 12, label));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && this.backLabel() != null) {
            this.back();
            return true;
        }
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
        graphics.fillGradient(0, 0, this.width, this.height, withAlpha(this.theme.veil(), 0.80F * f), withAlpha(0x000000, 0.93F * f));
        this.renderCircle(graphics, f);
    }

    /** The blood circle: two dotted rings, a pentagram and eight small marks, turning very slowly. */
    private void renderCircle(GuiGraphics graphics, float fade) {
        int cx = this.width / 2;
        int cy = this.listTop + this.listHeight / 2;
        float radius = Math.min(Math.max(this.listHeight / 2.0F + 30.0F, 70.0F), this.height / 2.0F - 6.0F);
        double turn = (Util.getMillis() % 240000L) / 240000.0D * Math.PI * 2.0D;

        int rune = this.theme.rune();
        int outer = withAlpha(rune, 0.30F * fade);
        int inner = withAlpha(rune, 0.16F * fade);
        int star = withAlpha(rune, 0.10F * fade);

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
        if (this.theme.clock()) {
            // A clock face: twelve hour marks and two hands, the minute hand turning about once a minute.
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6.0D;
                float len = i % 3 == 0 ? 7.0F : 3.5F;
                line(graphics, cx + Math.cos(a) * r, cy + Math.sin(a) * r,
                        cx + Math.cos(a) * (r - len), cy + Math.sin(a) * (r - len), outer);
            }
            double minute = (Util.getMillis() % 60000L) / 60000.0D * Math.PI * 2.0D - Math.PI / 2.0D;
            double hour = (Util.getMillis() % 720000L) / 720000.0D * Math.PI * 2.0D - Math.PI / 2.0D;
            line(graphics, cx, cy, cx + Math.cos(minute) * r * 0.80D, cy + Math.sin(minute) * r * 0.80D, star);
            line(graphics, cx, cy, cx + Math.cos(hour) * r * 0.50D, cy + Math.sin(hour) * r * 0.50D, inner);
            return;
        }
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
    private void divider(GuiGraphics graphics, int cx, int y, int halfWidth, float fade) {
        for (int i = 0; i < halfWidth; i++) {
            int c = withAlpha(this.theme.rune(), (1.0F - (float) i / halfWidth) * 0.55F * fade);
            graphics.fill(cx + 4 + i, y, cx + 5 + i, y + 1, c);
            graphics.fill(cx - 5 - i, y, cx - 4 - i, y + 1, c);
        }
        int d = withAlpha(this.theme.title(), fade);
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
            graphics.drawString(this.font, s, tx, titleY, textColor(this.theme.title(), f), false);
            tx += this.font.width(s) + spacing;
        }

        divider(graphics, cx, titleY + 14, 70, f);

        Component subtitle = this.subtitle().copy().withStyle(ChatFormatting.ITALIC);
        graphics.drawString(this.font, subtitle, cx - this.font.width(subtitle) / 2, titleY + 23, textColor(this.theme.subtitle(), f), false);

        super.render(graphics, mouseX, mouseY, partialTick);

        Component hint = this.hint();
        int hintY = this.backLabel() != null ? this.backY + 18 : this.listTop + this.listHeight + 12;
        graphics.drawString(this.font, hint, cx - this.font.width(hint) / 2, hintY, textColor(this.theme.hint(), f), false);
    }

    // ---------------------------------------------------------------- back link

    private class BackLink extends AbstractButton {
        BackLink(int x, int y, int w, int h, Component label) {
            super(x, y, w, h, label);
        }

        @Override
        public void onPress() {
            RitualScreen.this.back();
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            float f = RitualScreen.this.fade();
            boolean lit = this.isHoveredOrFocused();
            Font font = RitualScreen.this.font;
            graphics.drawString(font, this.getMessage(), this.getX() + this.getWidth() / 2 - font.width(this.getMessage()) / 2,
                    this.getY() + 2, textColor(lit ? RitualScreen.this.theme.numeralHover() : RitualScreen.this.theme.detail(), f), false);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
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
            Theme t = RitualScreen.this.theme;
            boolean lit = this.active && this.isHoveredOrFocused();
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();

            if (lit) {
                for (int i = 0; i < w; i++) {
                    float a = 0.30F * (1.0F - (float) i / w);
                    graphics.fill(x + i, y, x + i + 1, y + h, withAlpha(t.rowGlow(), a * f));
                }
                graphics.fill(x, y, x + 1, y + h, withAlpha(t.rowEdge(), f));
            }
            for (int i = 0; i < w; i++) {
                float a = (lit ? 0.45F : 0.18F) * (1.0F - Math.abs(i - w / 2.0F) / (w / 2.0F));
                graphics.fill(x + i, y + h - 1, x + i + 1, y + h, withAlpha(t.rune(), a * f));
            }

            Font font = RitualScreen.this.font;
            int textY = y + (h - 8) / 2;

            String numeral = this.index < NUMERALS.length ? NUMERALS[this.index] : String.valueOf(this.index + 1);
            graphics.drawString(font, numeral, x + 18 - font.width(numeral) / 2, textY,
                    textColor(lit ? t.numeralHover() : t.numeral(), f), false);

            int iconX = x + 32;
            int iconY = y + (h - 16) / 2;
            if (option.texture() != null) {
                graphics.blit(option.texture(), iconX, iconY, 16, 16, 0.0F, 0.0F, 18, 18, 18, 18);
            } else {
                graphics.renderItem(option.item(), iconX, iconY);
            }

            int nameColor = !this.active ? t.textDisabled() : lit ? t.textHover() : t.text();
            graphics.drawString(font, this.getMessage(), iconX + 24, textY, textColor(nameColor, f), lit);

            if (option.cooldown() > 0.0F) {
                int bx = x + w - 8 - COOLDOWN_BAR_WIDTH;
                int by = y + h / 2 - 1;
                graphics.fill(bx, by, bx + COOLDOWN_BAR_WIDTH, by + 3, withAlpha(t.barBack(), 0.9F * f));
                graphics.fill(bx, by, bx + Math.max(1, (int) (COOLDOWN_BAR_WIDTH * option.cooldown())), by + 3,
                        withAlpha(t.barFill(), f));
            } else if (option.detail() != null) {
                int dx = x + w - 8 - font.width(option.detail());
                graphics.drawString(font, option.detail(), dx, textY,
                        textColor(!this.active ? t.textDisabled() : lit ? t.detailHover() : t.detail(), f), false);
            }
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
