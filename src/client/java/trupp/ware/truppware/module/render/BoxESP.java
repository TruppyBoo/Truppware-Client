package trupp.ware.truppware.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventWorldRender;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.ModeSetting;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.ColourUtil;
import trupp.ware.util.Render3DUtil;

import java.util.List;

/**
 * Slick 3D box ESP — clean bounding-box outlines around players, with rainbow/health/static
 * colouring, optional tracers, and a subtle outer "glow" pass.
 */
public class BoxESP extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    public ModeSetting color    = new ModeSetting("Color", List.of("Rainbow", "Health", "Static"));
    public NumberSetting width  = new NumberSetting("Line Width", 0.5, 4.0, 1.6, 0.1);
    public NumberSetting alpha  = new NumberSetting("Opacity",    20,  255, 230, 5);
    public BooleanSetting glow  = new BooleanSetting("Glow", true);
    public BooleanSetting tracers = new BooleanSetting("Tracers", false);
    public BooleanSetting self  = new BooleanSetting("Include Self", false);

    public BoxESP() {
        super("BoxESP", Category.RENDER, "Slick 3D box ESP", -1);
        addSettings(color, width, alpha, glow, tracers, self);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventWorldRender event)) return;
        if (mc.level == null || mc.player == null) return;

        float pt = event.getPartialTick();
        float lw = (float) width.getNum();
        int a = (int) alpha.getNum();

        int i = 0;
        // Snapshot — avoids ConcurrentModificationException if the player list mutates mid-frame.
        for (AbstractClientPlayer p : new java.util.ArrayList<>(mc.level.players())) {
            if (p.isRemoved() || p.getHealth() <= 0) continue;
            if (!self.isEnabled() && p == mc.player) continue;

            // Interpolated position -> smooth, non-jittery box.
            double x = Mth.lerp(pt, p.xOld, p.getX());
            double y = Mth.lerp(pt, p.yOld, p.getY());
            double z = Mth.lerp(pt, p.zOld, p.getZ());
            AABB box = p.getBoundingBox().move(x - p.getX(), y - p.getY(), z - p.getZ());

            int rgb = colorFor(p, i) & 0xFFFFFF;
            int main = (a << 24) | rgb;

            // Subtle outer glow: a slightly larger, fainter, thicker box behind the main one.
            if (glow.isEnabled()) {
                int glowCol = ((a / 4) << 24) | rgb;
                Render3DUtil.drawBox(event, box.inflate(0.06), glowCol, lw * 2.4f);
            }

            Render3DUtil.drawBox(event, box, main, lw);

            if (tracers.isEnabled()) {
                Vector3fc fwd = mc.gameRenderer.getMainCamera().forwardVector();
                Vec3 cam = event.getCameraPos();
                double sx = cam.x + fwd.x();
                double sy = cam.y + fwd.y();
                double sz = cam.z + fwd.z();
                Render3DUtil.drawLine(event, sx, sy, sz,
                        box.getCenter().x, box.minY, box.getCenter().z, main, lw);
            }
            i++;
        }
    }

    private int colorFor(AbstractClientPlayer p, int index) {
        switch (color.getCurrentMode()) {
            case "Health": {
                float frac = Mth.clamp(p.getHealth() / Math.max(1f, p.getMaxHealth()), 0f, 1f);
                int rr = (int) ((1f - frac) * 255);
                int gg = (int) (frac * 255);
                return (rr << 16) | (gg << 8);
            }
            case "Static":
                return 0x00E5FF;
            default: // Rainbow
                return ColourUtil.getRainbow(index * 60, 3f);
        }
    }
}
