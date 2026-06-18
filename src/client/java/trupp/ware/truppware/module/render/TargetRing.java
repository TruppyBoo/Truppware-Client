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

public class TargetRing extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    // "3 block ring" = 3 blocks wide (radius 1.5). Adjustable.
    public NumberSetting radius   = new NumberSetting("Radius",   0.5, 6.0, 1.5, 0.1);
    public NumberSetting segments = new NumberSetting("Quality",  8,   64,  40,  1);
    public BooleanSetting self    = new BooleanSetting("Include Self", false);
    public BooleanSetting rainbow = new BooleanSetting("Rainbow", true);

    public TargetRing() {
        super("TargetRing", Category.RENDER, "Draws a ring around every player", -1);
        addSettings(radius, segments, self, rainbow);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventWorldRender event)) return;
        if (mc.level == null || mc.player == null) return;

        if(mc.level.players().isEmpty()) return;

        float pt = event.getPartialTick();
        int seg = (int) segments.getNum();
        double r = radius.getNum();

        int i = 0;
        // Snapshot the list — it can be mutated on another thread during a join, and iterating
        // the live list would throw ConcurrentModificationException.
        for (AbstractClientPlayer p : new java.util.ArrayList<>(mc.level.players())) {
            if (p.isRemoved() || p.getHealth() <= 0) continue;
            if (!self.isEnabled() && p == mc.player) continue;

            // Interpolated position for smooth (non-jittery) rendering.
            double x = Mth.lerp(pt, p.xOld, p.getX());
            double y = Mth.lerp(pt, p.yOld, p.getY());
            double z = Mth.lerp(pt, p.zOld, p.getZ());

            int color = rainbow.isEnabled()
                    ? (ColourUtil.getRainbow(i * 60, 3f) | 0xFF000000)
                    : 0xFF00E5FF;

            Render3DUtil.drawCircle(event, x, y, z, r, seg, color);
            i++;
        }
    }
}
