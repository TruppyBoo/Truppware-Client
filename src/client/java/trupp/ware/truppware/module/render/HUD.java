package trupp.ware.truppware.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventRender;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;

import java.awt.Color;

public class HUD extends Module {

    public HUD() {
        super("HUD", Category.RENDER, "Displays client logo", -1);
        toggled = true;
    }

    @Override
    public void onEvent(Event e, Timing times) {
        if (!(e instanceof EventRender event)) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        int x = 5; // top-left corner X
        int y = 5; // top-left corner Y

        // Draw client name/logo with gradient effect
        long time = System.currentTimeMillis();
        float hue = (time % 10000) / 10000f; // cycles every 10 seconds
        int color = Color.HSBtoRGB(hue, 0.8f, 1f);

        graphics.drawString(font, "§dTruppWare §7v6", x, y, color, false);
    }
}
