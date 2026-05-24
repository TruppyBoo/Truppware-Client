package trupp.ware.truppware.module.render.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import trupp.ware.truppware.module.Category;

public class ClickGui extends Screen {

    private final int startX = 30;
    private final int startY = 40;
    private final int boxWidth = 140;
    private final int boxHeight = 26;
    private final int padding = 4;

    private static int lastSelectedIndex = 0;
    private int selectedIndex = lastSelectedIndex;

    private float alpha = 0f;
    private boolean fadingIn = true;
    private boolean fadingOut = false;
    private final float fadeSpeed = 0.06f;

    public ClickGui() {
        super(Component.literal("TruppWare"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (fadingIn) {
            alpha += fadeSpeed;
            if (alpha >= 1f) { alpha = 1f; fadingIn = false; }
        } else if (fadingOut) {
            alpha -= fadeSpeed;
            if (alpha <= 0f) {
                alpha = 0f;
                Minecraft.getInstance().setScreen(null);
                return;
            }
        }

        Minecraft mc = Minecraft.getInstance();
        int a = (int)(alpha * 255);

        // Title
        int titleColor = (a << 24) | 0xFFFFFF;
        guiGraphics.drawString(mc.font, "§lTruppWare", startX, startY - 20, titleColor, false);

        int currentY = startY;

        for (int i = 0; i < Category.values().length; i++) {
            Category category = Category.values()[i];
            boolean selected = (i == selectedIndex);

            // Background — darker, semi-transparent
            int bgColor = ((int)(alpha * 200) << 24) | (selected ? 0x2A3450 : 0x181A20);
            guiGraphics.fill(startX, currentY, startX + boxWidth, currentY + boxHeight, bgColor);

            // Left accent bar — bright blue when selected
            if (selected) {
                int accentColor = (a << 24) | 0x4D7CFF;
                guiGraphics.fill(startX, currentY, startX + 3, currentY + boxHeight, accentColor);
            }

            // Bottom border line for subtle depth
            int borderColor = ((int)(alpha * 80) << 24) | 0xFFFFFF;
            guiGraphics.fill(startX, currentY + boxHeight - 1, startX + boxWidth, currentY + boxHeight, borderColor);

            // Text
            int textColor = selected ? ((a << 24) | 0xFFFFFF) : ((int)(alpha * 200) << 24 | 0xCCCCCC);
            guiGraphics.drawString(mc.font, category.name(), startX + 10, currentY + 9, textColor, false);

            currentY += boxHeight + padding;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int key = keyEvent.key();

        if (key == 256) { fadingOut = true; return true; }

        if (key == 265) {
            selectedIndex--;
            if (selectedIndex < 0) selectedIndex = Category.values().length - 1;
            return true;
        }
        if (key == 264) {
            selectedIndex++;
            if (selectedIndex >= Category.values().length) selectedIndex = 0;
            return true;
        }

        if (key == 257 || key == 335) {
            lastSelectedIndex = selectedIndex;
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new Modules(Category.values()[selectedIndex]));
            return true;
        }

        return super.keyPressed(keyEvent);
    }
}