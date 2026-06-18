package trupp.ware.truppware.module.modules.MOVEMENT;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;

/**
 * Velocity-based flight — drives the player's motion vector directly instead of using Minecraft's
 * creative {@code abilities.flying} (which servers flag as creative flight). WASD moves along your
 * look direction, Jump/Sneak go up/down, and releasing the keys hovers.
 */
public class Fly extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    public NumberSetting speed    = new NumberSetting("Speed", 0.1, 5.0, 0.8, 0.1);
    public NumberSetting vertical = new NumberSetting("Vertical", 0.1, 5.0, 0.8, 0.1);

    public Fly() {
        super("Flight", Category.MOVEMENT, "Velocity-based flight", GLFW.GLFW_KEY_G);
        addSettings(speed, vertical);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventTick) || time == Timing.POST) return;
        if (mc.player == null) return;

        double sp = speed.getNum();
        double vert = vertical.getNum();

        float forward = (key(mc.options.keyUp) ? 1f : 0f)   - (key(mc.options.keyDown) ? 1f : 0f);
        float strafe  = (key(mc.options.keyLeft) ? 1f : 0f)  - (key(mc.options.keyRight) ? 1f : 0f); // left positive (MC)

        double yawRad = Math.toRadians(mc.player.getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double vx = 0, vz = 0;
        if (forward != 0 || strafe != 0) {
            double mag = Math.sqrt(forward * forward + strafe * strafe);
            double f = forward / mag, s = strafe / mag;
            vx = (s * cos - f * sin) * sp;
            vz = (f * cos + s * sin) * sp;
        }

        double vy = 0;
        if (key(mc.options.keyJump))  vy += vert;
        if (key(mc.options.keyShift)) vy -= vert;

        mc.player.setDeltaMovement(vx, vy, vz);
        mc.player.fallDistance = 0;
    }

    private boolean key(net.minecraft.client.KeyMapping k) {
        return k.isDown();
    }

    @Override
    public void OnDisable() {
        if (mc.player != null) {
            mc.player.getAbilities().flying = false;
            mc.player.fallDistance = 0;
        }
    }
}
