package trupp.ware.truppware.module.render.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.truppware.module.settings.ModeSetting;
import trupp.ware.truppware.module.settings.Setting;

public class Settings extends Screen {

    private final Module module;
    private int selectedIndex = 0;

    private final int startX = 30;
    private final int startY = 40;
    private final int boxWidth = 200;
    private final int boxHeight = 24;
    private final int padding = 3;

    private float alpha = 0f;
    private final float fadeSpeed = 0.06f;

    public Settings(Module module) {
        super(Component.literal(module.name + " Settings"));
        this.module = module;
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

        // Title
        int titleColor = (a << 24) | 0xFFFFFF;
        guiGraphics.drawString(mc.font, "§l" + module.name, startX, startY - 20, titleColor, false);

        int currentY = startY;

        for (int i = 0; i < module.getSettings().size(); i++) {
            Setting setting = module.getSettings().get(i);
            boolean selected = (i == selectedIndex);

            // Background
            int bgColor = ((int)(alpha * 200) << 24) | (selected ? 0x2A3450 : 0x181A20);
            guiGraphics.fill(startX, currentY, startX + boxWidth, currentY + boxHeight, bgColor);

            // Left accent
            if (selected) {
                int accent = (a << 24) | 0x4D7CFF;
                guiGraphics.fill(startX, currentY, startX + 3, currentY + boxHeight, accent);
            }

            // Bottom divider
            int borderColor = ((int)(alpha * 80) << 24) | 0xFFFFFF;
            guiGraphics.fill(startX, currentY + boxHeight - 1, startX + boxWidth, currentY + boxHeight, borderColor);

            // Setting name (left)
            int nameColor = selected ? ((a << 24) | 0xFFFFFF) : ((int)(alpha * 200) << 24 | 0xCCCCCC);
            guiGraphics.drawString(mc.font, setting.name, startX + 10, currentY + 8, nameColor, false);

            // Value (right side)
            String value = formatValue(setting);
            int valueColor;
            if (setting instanceof BooleanSetting b) {
                valueColor = b.isEnabled() ? ((a << 24) | 0x4DFF8C) : ((a << 24) | 0xFF6666);
            } else {
                valueColor = (a << 24) | 0x4D7CFF;
            }

            int valueWidth = mc.font.width(value);
            guiGraphics.drawString(mc.font, value, startX + boxWidth - valueWidth - 10, currentY + 8, valueColor, false);

            currentY += boxHeight + padding;
        }

        // Help text
        int helpColor = ((int)(alpha * 120) << 24) | 0x888888;
        guiGraphics.drawString(mc.font, "↑↓ navigate  •  ←→ adjust  •  ENTER toggle  •  ESC back", startX, currentY + 8, helpColor, false);
    }

    private String formatValue(Setting setting) {
        if (setting instanceof BooleanSetting b) {
            return b.isEnabled() ? "ON" : "OFF";
        }
        if (setting instanceof NumberSetting n) {
            double val = n.getNum();
            // Round to 2 decimals
            double rounded = Math.round(val * 100.0) / 100.0;
            // Display as integer if whole number
            if (rounded == (long) rounded) {
                return String.valueOf((long) rounded);
            }
            return String.format("%.2f", rounded);
        }
        if (setting instanceof ModeSetting m) {
            return m.getCurrentMode();
        }
        return "";
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int key = keyEvent.key();

        if (key == 256) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }

        if (key == 265) {
            selectedIndex--;
            if (selectedIndex < 0) selectedIndex = module.getSettings().size() - 1;
            return true;
        }
        if (key == 264) {
            selectedIndex++;
            if (selectedIndex >= module.getSettings().size()) selectedIndex = 0;
            return true;
        }

        if (!module.getSettings().isEmpty()) {
            Setting setting = module.getSettings().get(selectedIndex);

            if (key == 257 || key == 335) {
                if (setting instanceof BooleanSetting bool) {
                    bool.setEnabled(!bool.isEnabled());
                    return true;
                }
                if (setting instanceof ModeSetting mode) {
                    mode.cycle();
                    return true;
                }
            }

            if (key == 262) {
                if (setting instanceof NumberSetting num) {
                    num.increase();
                    return true;
                }
                if (setting instanceof ModeSetting mode) {
                    mode.cycle();
                    return true;
                }
            }

            if (key == 263) {
                if (setting instanceof NumberSetting num) {
                    num.decrease();
                    return true;
                }
                if (setting instanceof ModeSetting mode) {
                    mode.cycle();
                    return true;
                }
            }
        }

        return super.keyPressed(keyEvent);
    }
}