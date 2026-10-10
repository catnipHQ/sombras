package com.livrodassombras.client;

import com.livrodassombras.network.ModNetwork;
import com.livrodassombras.network.SelectWeaponPacket;
import com.livrodassombras.summon.BloodWeapons;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The menu opened by the Livro do Sangue: one button per weapon. */
public class BloodBookScreen extends Screen {
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 24;
    private static final int GAP = 6;

    private final List<Item> weapons = BloodWeapons.menuOrder();
    private int firstButtonY;

    public BloodBookScreen() {
        super(Component.translatable("gui.livro_das_sombras.livro_do_sangue.title"));
    }

    @Override
    protected void init() {
        int totalHeight = this.weapons.size() * BUTTON_HEIGHT + (this.weapons.size() - 1) * GAP;
        this.firstButtonY = this.height / 2 - totalHeight / 2 + 10;
        int x = this.width / 2 - BUTTON_WIDTH / 2 + 10;

        for (int i = 0; i < this.weapons.size(); i++) {
            final int index = i;
            Item weapon = this.weapons.get(i);
            this.addRenderableWidget(Button.builder(weapon.getDescription(), button -> choose(index))
                    .bounds(x, this.firstButtonY + i * (BUTTON_HEIGHT + GAP), BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    private void choose(int index) {
        ModNetwork.CHANNEL.sendToServer(new SelectWeaponPacket(index));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.firstButtonY - 38, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.livro_das_sombras.livro_do_sangue.cost"),
                this.width / 2, this.firstButtonY - 24, 0xFF5555);

        int iconX = this.width / 2 - BUTTON_WIDTH / 2 - 14;
        for (int i = 0; i < this.weapons.size(); i++) {
            graphics.renderItem(new ItemStack(this.weapons.get(i)),
                    iconX, this.firstButtonY + i * (BUTTON_HEIGHT + GAP) + (BUTTON_HEIGHT - 16) / 2);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
