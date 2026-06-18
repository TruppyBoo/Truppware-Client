package trupp.ware.truppware.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventWorldRender;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.ColourUtil;
import trupp.ware.util.Render3DUtil;

/**
 * A stylish conical "rice hat" over every player's head — drawn as a clean wireframe cone (brim
 * ring + a mid ring + ribs up to the apex) using the same line renderer as the ESP, so it can't
 * conflict with the other render modules. Straw-coloured by default, or rainbow.
 */
public class RiceHat extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    public NumberSetting radius = new NumberSetting("Brim", 0.3, 1.2, 0.6, 0.05);
    public NumberSetting height = new NumberSetting("Height", 0.1, 1.0, 0.34, 0.02);
    public NumberSetting width  = new NumberSetting("LineWidth", 0.5, 4.0, 1.6, 0.1);
    public NumberSetting ribs   = new NumberSetting("Ribs", 4, 16, 10, 1);
    public BooleanSetting rainbow = new BooleanSetting("Rainbow", false);
    public BooleanSetting self   = new BooleanSetting("Include Self", true);

    public RiceHat() {
        super("RiceHat", Category.RENDER, "Asian rice hats over players' heads", -1);
        addSettings(radius, height, width, ribs, rainbow, self);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventWorldRender event)) return;
        if (mc.level == null || mc.player == null) return;

        float pt = event.getPartialTick();
        double r = radius.getNum();
        double h = height.getNum();
        float lw = (float) width.getNum();
        int ribCount = (int) ribs.getNum();

        int i = 0;
        for (AbstractClientPlayer p : new java.util.ArrayList<>(mc.level.players())) {
            if (p.isRemoved() || p.getHealth() <= 0) continue;
            if (!self.isEnabled() && p == mc.player) continue;

            double x = Mth.lerp(pt, p.xOld, p.getX());
            double y = Mth.lerp(pt, p.yOld, p.getY());
            double z = Mth.lerp(pt, p.zOld, p.getZ());

            double baseY = y + p.getBbHeight() * 0.96 + 0.04;   // brim sits just above the head
            double apexY = baseY + h;

            int rgb = rainbow.isEnabled() ? (ColourUtil.getRainbow(i * 50, 3f) & 0xFFFFFF) : 0xE0C068;
            int main = 0xFF000000 | rgb;
            int dim  = 0xFF000000 | darken(rgb, 0.6f);

            // brim ring + a smaller mid ring for the cone shape
            Render3DUtil.drawCircle(event, x, baseY, z, r, 28, main);
            Render3DUtil.drawCircle(event, x, baseY + h * 0.45, z, r * 0.55, 24, dim);

            // ribs from the apex down to the brim
            for (int k = 0; k < ribCount; k++) {
                double ang = (Math.PI * 2.0) * k / ribCount;
                double bx = x + Math.cos(ang) * r;
                double bz = z + Math.sin(ang) * r;
                Render3DUtil.drawLine(event, x, apexY, z, bx, baseY, bz, main, lw);
            }
            i++;
        }
    }

    private int darken(int rgb, float f) {
        int rr = (int) (((rgb >> 16) & 0xFF) * f);
        int gg = (int) (((rgb >> 8) & 0xFF) * f);
        int bb = (int) ((rgb & 0xFF) * f);
        return (rr << 16) | (gg << 8) | bb;
    }
}
