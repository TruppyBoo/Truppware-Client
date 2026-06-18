package trupp.ware.truppware.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventAttack;
import trupp.ware.event.events.EventRender;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.util.Fonts;
import trupp.ware.util.Projection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Floating damage numbers: when you hit something, the damage you dealt pops up at the hit point
 * in 3D, briefly, rendered with the custom font — rises, scales in, then fades.
 */
public class DamageTag extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    // entityId -> health snapshot taken when we attacked it (waiting for the drop to register)
    private final Map<Integer, Pending> pending = new HashMap<>();
    private final List<Popup> popups = new ArrayList<>();

    private static final long LIFETIME = 1200L; // ms a popup stays

    public DamageTag() {
        super("DamageTag", Category.RENDER, "Shows floating damage numbers when you hit", -1);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (e instanceof EventAttack ev) {
            if (mc.player == null) return;
            if (ev.getEntity() instanceof LivingEntity le && le != mc.player) {
                // snapshot effective health (incl. absorption) before the hit lands
                pending.put(le.getId(),
                        new Pending(le.getHealth() + le.getAbsorptionAmount(),
                                System.currentTimeMillis() + 900));
            }
            return;
        }

        if (e instanceof EventTick) {
            if (time == Timing.POST || mc.player == null || mc.level == null) return;
            detectDamage();
            return;
        }

        if (e instanceof EventRender ev) {
            renderPopups(ev.getGuiGraphics());
        }
    }

    private void detectDamage() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Pending>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Pending> en = it.next();
            Pending p = en.getValue();
            Entity ent = mc.level.getEntity(en.getKey());

            if (!(ent instanceof LivingEntity le)) {
                if (now > p.expire) it.remove();   // entity gone / no read
                continue;
            }

            float cur = le.getHealth() + le.getAbsorptionAmount();
            if (cur < p.health - 0.01f) {
                spawn(le, p.health - cur);
                it.remove();
            } else if (now > p.expire) {
                it.remove();
            }
        }
    }

    private void spawn(LivingEntity le, float dmg) {
        double x = le.getX() + (Math.random() - 0.5) * 0.4;
        double y = le.getY() + le.getBbHeight() * 0.85 + 0.3;
        double z = le.getZ() + (Math.random() - 0.5) * 0.4;
        popups.add(new Popup(dmg, x, y, z, System.currentTimeMillis()));
        while (popups.size() > 40) popups.remove(0);
    }

    private void renderPopups(GuiGraphics g) {
        if (popups.isEmpty() || !Fonts.MAIN.isReady()) return;

        long now = System.currentTimeMillis();
        int sw = g.guiWidth();
        int sh = g.guiHeight();
        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        Iterator<Popup> it = popups.iterator();
        while (it.hasNext()) {
            Popup p = it.next();
            long age = now - p.birth;
            if (age > LIFETIME) { it.remove(); continue; }

            float t = age / (float) LIFETIME;            // 0..1
            double rise = t * 0.7;                         // floats up

            Vector3f screen = Projection.worldToScreen(p.x, p.y + rise, p.z, sw, sh);
            if (screen == null) continue;

            // size: shrink with distance, then a quick "pop-in" overshoot at the start
            double dist = cam.distanceTo(new Vec3(p.x, p.y, p.z));
            float scale = (float) (2 * (4.0 / (4.0 + dist)));
            scale *= 1f + 0.7f * (float) Math.exp(-age / 90.0);
            scale = Math.max(0.12f, Math.min(scale, 0.8f));

            // fade out over the last third
            float alpha = t < 0.65f ? 1f : 1f - (t - 0.65f) / 0.35f;
            int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255);

            String text = fmt(p.damage);
            int color = damageColor(p.damage, a);

            float w = Fonts.MAIN.getWidth(text, scale);
            float h = Fonts.MAIN.getHeight(scale);
            Fonts.MAIN.drawStringWithShadow(g, text, screen.x - w / 2f, screen.y - h / 2f, color, scale);
        }
    }

    private String fmt(float d) {
        if (d >= 10f) return String.valueOf(Math.round(d));
        double r = Math.round(d * 10.0) / 10.0;
        if (r == (long) r) return String.valueOf((long) r);
        return String.format("%.1f", r);
    }

    /** White/gold for small hits, deepening to red for big ones. */
    private int damageColor(float dmg, int alpha) {
        float frac = Math.max(0f, Math.min(1f, dmg / 12f));
        int r = 255;
        int gg = (int) (230 - frac * 180);
        int b = (int) (120 - frac * 70);
        return (alpha << 24) | (r << 16) | (gg << 8) | b;
    }

    @Override
    public void OnDisable() {
        pending.clear();
        popups.clear();
    }

    private static final class Pending {
        final float health;
        final long expire;
        Pending(float health, long expire) { this.health = health; this.expire = expire; }
    }

    private static final class Popup {
        final float damage;
        final double x, y, z;
        final long birth;
        Popup(float damage, double x, double y, double z, long birth) {
            this.damage = damage; this.x = x; this.y = y; this.z = z; this.birth = birth;
        }
    }
}
