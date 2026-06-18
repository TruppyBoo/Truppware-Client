package trupp.ware.truppware.module.modules.MOVEMENT;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.NumberSetting;

/**
 * Velocity bunny-hop: auto-jumps and drives your horizontal motion vector directly so you move
 * fast and can air-strafe — turn your view (and hold strafe) mid-air to redirect your momentum.
 */
public class Speed extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    public NumberSetting speed  = new NumberSetting("Speed", 0.1, 2.0, 0.42, 0.01); // blocks/tick
    public NumberSetting jump   = new NumberSetting("JumpHeight", 0.1, 1.0, 0.42, 0.01);
    public BooleanSetting autoJump = new BooleanSetting("AutoJump", true);

    public Speed() {
        super("Speed", Category.MOVEMENT, "Velocity bhop / air-strafe", 0);
        addSettings(speed, jump, autoJump);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventTick) || time == Timing.POST) return;
        if (mc.player == null) return;
        if (mc.player.isInWater() || mc.player.onClimbable()) return;

        float forward = (key(mc.options.keyUp) ? 1f : 0f)  - (key(mc.options.keyDown) ? 1f : 0f);
        float strafe  = (key(mc.options.keyLeft) ? 1f : 0f) - (key(mc.options.keyRight) ? 1f : 0f); // left positive

        if (forward == 0 && strafe == 0) return; // only act while trying to move

        double yawRad = Math.toRadians(mc.player.getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double mag = Math.sqrt(forward * forward + strafe * strafe);
        double f = forward / mag, s = strafe / mag;
        double dx = (s * cos - f * sin);
        double dz = (f * cos + s * sin);

        double sp = speed.getNum();
        Vec3 m = mc.player.getDeltaMovement();
        double vy = m.y;

        if (mc.player.onGround() && autoJump.isEnabled()) {
            vy = jump.getNum();
        }

        // Drive horizontal momentum toward where we're aiming — this is the air-strafe control.
        mc.player.setDeltaMovement(dx * sp, vy, dz * sp);
        mc.player.fallDistance = 0;
    }

    private boolean key(net.minecraft.client.KeyMapping k) {
        return k.isDown();
    }
}
