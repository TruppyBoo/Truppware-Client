package trupp.ware.truppware.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventRender;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.util.Fonts;
import trupp.ware.util.Images;

import java.awt.Color;

public class HUD extends Module {

    public HUD() {
        super("HUD", Category.RENDER, "Displays client logo", -1);
        toggled = true;
    }

    @Override
    public void onEvent(Event e, Timing times) {
        if (!(e instanceof EventRender event)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        if (!Fonts.MAIN.isReady()) return;

        float x = 6f;
        float y = 5f;
        float scale = 0.5f;
        float scale2 = 1f;

        // Client logo (left of the name) — gives it that real-client look.
        float textX = x;
        if (Images.LOGO.isReady()) {
            float logo = Fonts.MAIN.getHeight(scale2) + 2f;   // match the text height
            Images.LOGO.draw(graphics, x, y - 1f, logo, logo);
            textX = x + logo + 4f;
        }

        long time = System.currentTimeMillis();
        float hue = (time % 10000) / 10000f; // cycles every 10 seconds
        int color = Color.HSBtoRGB(hue, 0.55f, 1f) | 0xFF000000;

        Fonts.MAIN.drawStringWithShadow(graphics, "TruppWare", textX, y, color, scale);
        float w = Fonts.MAIN.getWidth("TruppWare ", scale);
        Fonts.MAIN.drawStringWithShadow(graphics, "v6", textX + w, y, 0xFF9AA0B5, scale);
    }
}
