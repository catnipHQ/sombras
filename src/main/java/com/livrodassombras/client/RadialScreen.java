package com.livrodassombras.client;

import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.network.SelectRitualPacket;
import com.livrodassombras.network.SelectWeaponPacket;
import com.livrodassombras.ritual.BookPath;
import com.livrodassombras.ritual.ClientRitualCooldowns;
import com.livrodassombras.ritual.MinorRitual;
import com.livrodassombras.ritual.RitualBook;
import com.livrodassombras.summon.BloodWeapons;
import com.livrodassombras.summon.SpiritSummon;
import com.livrodassombras.summon.TimeSummon;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The selection wheel (key R) for a ritual book's locked section (red for Blood, grey for Time, light blue for Spirit). Move the mouse towards an
 * option and release R (or click) to choose it. Esc, or R again with nothing chosen, closes it.
 */
public class RadialScreen extends Screen {
    private static final int RADIUS = 64;
    private static final int DEAD_ZONE = 16;
    /** A cooldown to show on an entry: its id and full length (null id: none). */
    private record Entry(Component name, ItemStack item, @Nullable ResourceLocation texture,
                         @Nullable String cooldownId, int cooldownTicks) {}

    private final int section;
    private final RitualScreen.Theme theme;
    private final List<Entry> entries = new ArrayList<>();
    private int selected = -1;

    public RadialScreen(int section, BookPath path) {
        super(Component.translatable("gui.livro_das_sombras.roda.title"));
        this.section = section;
        this.theme = BookScreens.theme(path);
        if (section == RitualBook.SECTION_RITUAIS) {
            for (MinorRitual ritual : MinorRitual.of(path)) {
                this.entries.add(new Entry(ritual.displayName(), ritual.icon(), null, ritual.id(), ritual.cooldownTicks()));
            }
        } else if (path == BookPath.SPIRIT) {
            for (SpiritSummon summon : SpiritSummon.values()) {
                this.entries.add(new Entry(summon.displayName(), summon.icon(), null, summon.id(), summon.cooldownTicks()));
            }
        } else if (path == BookPath.TIME) {
            for (TimeSummon summon : TimeSummon.values()) {
                this.entries.add(new Entry(summon.displayName(), summon.icon(), null,
                        summon.cooldownTicks() > 0 ? summon.id() : null, summon.cooldownTicks()));
            }
        } else {
            for (Item weapon : BloodWeapons.menuOrder()) {
                this.entries.add(new Entry(weapon.getDescription(), new ItemStack(weapon), null, null, 0));
            }
            this.entries.add(new Entry(Component.translatable("entity.livro_das_sombras.ser_de_sangue"),
                    ItemStack.EMPTY, BloodBookScreen.BEING_ICON, null, 0));
        }
    }

    private static int alpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private int cx() {
        return this.width / 2;
    }

    private int cy() {
        return this.height / 2;
    }

    private double angleOf(int i) {
        return -Math.PI / 2.0D + i * Math.PI * 2.0D / this.entries.size();
    }

    private int pick(double mouseX, double mouseY) {
        double dx = mouseX - this.cx();
        double dy = mouseY - this.cy();
        if (dx * dx + dy * dy < DEAD_ZONE * DEAD_ZONE) {
            return -1;
        }
        double a = Math.atan2(dy, dx) + Math.PI / 2.0D;
        double step = Math.PI * 2.0D / this.entries.size();
        int i = (int) Math.round(a / step);
        return Math.floorMod(i, this.entries.size());
    }

    private void choose() {
        if (this.selected < 0) {
            return;
        }
        if (this.section == RitualBook.SECTION_RITUAIS) {
            ModNetwork.CHANNEL.sendToServer(new SelectRitualPacket(this.selected));
        } else {
            ModNetwork.CHANNEL.sendToServer(new SelectWeaponPacket(this.selected));
        }
        this.onClose();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.WHEEL.matches(keyCode, scanCode) && this.selected >= 0) {
            this.choose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.WHEEL.matches(keyCode, scanCode)) {
            this.onClose(); // tap R again to put the wheel away
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.selected = this.pick(mouseX, mouseY);
        if (this.selected >= 0) {
            this.choose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        if (ClientEvents.heldBook().isEmpty()) {
            this.onClose(); // the book left the hand
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // Light veil: the fight stays visible behind the wheel.
        graphics.fillGradient(0, 0, this.width, this.height, alpha(this.theme.wheelVeil(), 0x55), 0x66000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        this.selected = this.pick(mouseX, mouseY);
        int cx = this.cx();
        int cy = this.cy();
        long now = ClientEvents.gameTime();

        // Two dotted rings.
        int rune = this.theme.rune();
        ring(graphics, cx, cy, RADIUS + 18, alpha(rune, 0x55));
        ring(graphics, cx, cy, DEAD_ZONE + 2, alpha(rune, 0x40));

        for (int i = 0; i < this.entries.size(); i++) {
            Entry entry = this.entries.get(i);
            double a = this.angleOf(i);
            int x = cx + (int) Math.round(Math.cos(a) * RADIUS);
            int y = cy + (int) Math.round(Math.sin(a) * RADIUS);
            boolean lit = i == this.selected;

            graphics.fill(x - 13, y - 13, x + 13, y + 13, lit ? alpha(this.theme.rowGlow(), 0xAA) : alpha(this.theme.wheelCell(), 0x88));
            int border = lit ? alpha(this.theme.numeralHover(), 0xFF) : alpha(rune, 0x66);
            graphics.fill(x - 13, y - 13, x + 13, y - 12, border);
            graphics.fill(x - 13, y + 12, x + 13, y + 13, border);
            graphics.fill(x - 13, y - 13, x - 12, y + 13, border);
            graphics.fill(x + 12, y - 13, x + 13, y + 13, border);

            if (entry.texture() != null) {
                graphics.blit(entry.texture(), x - 8, y - 8, 16, 16, 0.0F, 0.0F, 18, 18, 18, 18);
            } else {
                graphics.renderItem(entry.item(), x - 8, y - 8);
            }

            if (entry.cooldownId() != null) {
                float cooldown = ClientRitualCooldowns.fraction(entry.cooldownId(), entry.cooldownTicks(), now);
                if (cooldown > 0.0F) {
                    graphics.fill(x - 12, y - 12, x + 12, y + 12, 0x88000000);
                    graphics.fill(x - 10, y + 9, x + 10, y + 11, alpha(this.theme.barBack(), 0xFF));
                    graphics.fill(x - 10, y + 9, x - 10 + Math.max(1, (int) (20 * cooldown)), y + 11, alpha(this.theme.barFill(), 0xFF));
                }
            }
        }

        // Centre: what is under the mouse.
        Component label = this.selected >= 0 ? this.entries.get(this.selected).name()
                : Component.translatable("gui.livro_das_sombras.roda.hint", ModKeys.WHEEL.getTranslatedKeyMessage());
        int color = this.selected >= 0 ? this.theme.textHover() : this.theme.detail();
        graphics.drawCenteredString(this.font, label, cx, cy - 4, color);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static void ring(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        int dots = Math.max(16, (int) (Math.PI * 2.0D * radius / 4.0D));
        for (int i = 0; i < dots; i++) {
            double a = i * Math.PI * 2.0D / dots;
            int x = cx + Mth.floor(Math.cos(a) * radius);
            int y = cy + Mth.floor(Math.sin(a) * radius);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }
}
