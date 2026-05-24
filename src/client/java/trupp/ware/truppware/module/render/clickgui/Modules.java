package trupp.ware.truppware.module.render.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;

public class Modules extends Screen {

    private final Category category;

    private final int startX = 30;
    private final int startY = 40;
    private final int boxWidth = 160;
    private final int boxHeight = 24;
    private final int padding = 3;

    private int selectedIndex = 0;
    private float alpha = 0f;
    private final float fadeSpeed = 0.06f;

    public Modules(Category category) {
        super(Component.literal("Modules"));
        this.category = category;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (alpha < 1f) {
            alpha += fadeSpeed;
            if (alpha > 1f) alpha = 1f;
        }

        Minecraft mc = Minecraft.getInstance();
        int a = (int)(alpha * 255);

        // Category title
        int titleColor = (a << 24) | 0xFFFFFF;
        guiGraphics.drawString(mc.font, "§l" + category.name(), startX, startY - 20, titleColor, false);

        int currentY = startY;
        var modules = Manager.trupp.getModuleByCat(category);

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            boolean selected = (i == selectedIndex);
            boolean enabled = module.toggled;

            // Background
            int bgColor = ((int)(alpha * 200) << 24) | (selected ? 0x2A3450 : 0x181A20);
            guiGraphics.fill(startX, currentY, startX + boxWidth, currentY + boxHeight, bgColor);

            // Left accent — green if enabled, blue if just selected
            if (enabled) {
                int accent = (a << 24) | (selected ? 0x4DFF8C : 0x2DBB60);
                guiGraphics.fill(startX, currentY, startX + 3, currentY + boxHeight, accent);
            } else if (selected) {
                int accent = (a << 24) | 0x4D7CFF;
                guiGraphics.fill(startX, currentY, startX + 3, currentY + boxHeight, accent);
            }

            // Bottom divider
            int borderColor = ((int)(alpha * 80) << 24) | 0xFFFFFF;
            guiGraphics.fill(startX, currentY + boxHeight - 1, startX + boxWidth, currentY + boxHeight, borderColor);

            // Module name
            int textColor;
            if (enabled) textColor = (a << 24) | 0xFFFFFF;
            else if (selected) textColor = (a << 24) | 0xEEEEEE;
            else textColor = ((int)(alpha * 180) << 24) | 0xAAAAAA;

            guiGraphics.drawString(mc.font, module.name, startX + 10, currentY + 8, textColor, false);

            // ON indicator on right
            if (enabled) {
                int onColor = (a << 24) | 0x4DFF8C;
                guiGraphics.drawString(mc.font, "•", startX + boxWidth - 12, currentY + 8, onColor, false);
            }

            currentY += boxHeight + padding;
        }

        // Help text at bottom
        int helpColor = ((int)(alpha * 120) << 24) | 0x888888;
        guiGraphics.drawString(mc.font, "ENTER toggle  •  → settings  •  ESC back", startX, currentY + 8, helpColor, false);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int key = keyEvent.key();
        var modules = Manager.trupp.getModuleByCat(category);

        if (key == 256) {
            Minecraft.getInstance().setScreen(new ClickGui());
            return true;
        }

        if (key == 265) {
            selectedIndex--;
            if (selectedIndex < 0) selectedIndex = modules.size() - 1;
            return true;
        }
        if (key == 264) {
            selectedIndex++;
            if (selectedIndex >= modules.size()) selectedIndex = 0;
            return true;
        }

        if (key == 257 || key == 335) {
            if (!modules.isEmpty()) modules.get(selectedIndex).toggle();
            return true;
        }

        if (key == 262) {
            if (!modules.isEmpty()) {
                Module selected = modules.get(selectedIndex);
                Minecraft.getInstance().setScreen(new Settings(selected));
            }
            return true;
        }

        return super.keyPressed(keyEvent);
    }
}