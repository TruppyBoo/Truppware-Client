package trupp.ware.truppware.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventWorldRender;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.COMBAT.Aura;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.ColourUtil;
import trupp.ware.util.Render3DUtil;

/**
 * Draws a little circle at the centre of whatever target Aura is currently attacking.
 */
public class TargetCircle extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    public NumberSetting radius   = new NumberSetting("Radius",   0.1, 1.5, 0.4, 0.05);
    public NumberSetting segments = new NumberSetting("Quality",  6,   48,  30,  1);
    public NumberSetting width    = new NumberSetting("Thickness", 1,  4,   2,   0.5);
    public BooleanSetting pulse   = new BooleanSetting("Pulse", true);
    public BooleanSetting rainbow = new BooleanSetting("Rainbow", true);

    public TargetCircle() {
        super("TargetCircle", Category.RENDER, "Circle on Aura's current target", -1);
        addSettings(radius, segments, width, pulse, rainbow);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventWorldRender event)) return;
        if (mc.level == null || mc.player == null) return;

        // Only when Aura is locked onto a (live) target.
        if (!Aura.enabled) return;
        Player target = Aura.currentTarget;
        if (target == null || target.isRemoved() || target.isDeadOrDying()) return;

        float pt = event.getPartialTick();
        double x = Mth.lerp(pt, target.xOld, target.getX());
        double y = Mth.lerp(pt, target.yOld, target.getY()) + target.getBbHeight() * 0.5; // body centre
        double z = Mth.lerp(pt, target.zOld, target.getZ());

        double r = radius.getNum();
        if (pulse.isEnabled()) {
            r *= 0.85 + 0.15 * Math.sin(System.currentTimeMillis() / 180.0); // gentle breathe
        }

        int color = rainbow.isEnabled()
                ? (ColourUtil.getRainbow(0, 3f) | 0xFF000000)
                : 0xFF00E5FF;

        Render3DUtil.drawBillboardCircle(event, x, y, z, r, (int) segments.getNum(), color, (float) width.getNum());
    }
}
