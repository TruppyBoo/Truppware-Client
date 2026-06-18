package trupp.ware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventMovementFix;
import trupp.ware.event.events.EventPacket;
import trupp.ware.event.events.PacketDih;
import trupp.ware.event.events.Timing;

public class RotationUtil {



    private static final Minecraft mc = Minecraft.getInstance();

    public static float serverYaw;
    public static float serverPitch;

    /**
     * True while the silent rotation should be sent to the server / shown on the model. Stays true
     * during the smooth-out after a module stops aiming, so disabling Aura/Scaffold (or losing a
     * target) eases back to your real rotation instead of snapping. All the rotation mixins gate
     * on this.
     */
    public static boolean active;

    // Separately-eased values for the third-person model so nothing snaps on release: the head
    // eases to your real look yaw, the body to your movement direction, the pitch to real.
    public static float renderHeadYaw;
    public static float renderBodyYaw;
    public static float renderPitch;

    /** A module called {@link #set} this frame (i.e. it is actively aiming). */
    private static boolean claimed;

    public static float getServerYaw(){
        return serverYaw;
    }

    public static float getServerPitch(){
        return serverPitch;
    }

    public static void setServerYaw(float yaw){
        serverYaw = yaw;
    }

    public static void setServerPitch(float yaw){
        serverPitch = yaw;
    }

    /** Called by a module each frame it wants to silently aim. */
    public static void set(float yaw, float pitch) {
        serverYaw = yaw;
        serverPitch = pitch;
        active = true;
        claimed = true;
    }

    /**
     * Run once per render frame AFTER modules. If a module aimed this frame, keep going. Otherwise,
     * if we're still active, ease the server rotation back to the player's real rotation and then
     * release (active = false) so we return to normal aiming.
     */
    public static void update() {
        var p = mc.player;
        if (p == null) { active = false; claimed = false; return; }

        if (claimed) {            // a module is driving the rotation this frame -> aim everything
            claimed = false;
            renderHeadYaw = serverYaw;
            renderBodyYaw = serverYaw;
            renderPitch   = serverPitch;
            return;
        }
        if (!active) {            // idle — track the real rotation
            serverYaw   = p.getYRot();
            serverPitch = p.getXRot();
            renderHeadYaw = p.yHeadRot;
            renderBodyYaw = p.yBodyRot;
            renderPitch   = p.getXRot();
            return;
        }

        // Smooth-out: ease the sent rotation toward your real look, and ease the render model
        // toward its vanilla targets (head -> look, body -> movement fdirection) , then release.
        float realYaw    = p.getYRot();
        float realPitch  = p.getXRot();
        float bodyTarget = p.yBodyRot;     // body natu drally follows movement direction
        float headTarget = p.yHeadRot;     // head follows your look (vanilla, clamped to body)

        serverYaw     = approach(serverYaw,     realYaw,    0.12f);
        serverPitch   = approach(serverPitch,   realPitch,  0.12f);
        renderHeadYaw = approach(renderHeadYaw, headTarget, 0.12f);
        renderBodyYaw = approach(renderBodyYaw, bodyTarget, 0.12f);
        renderPitch   = approach(renderPitch,   realPitch,  0.12f);

        boolean done = near(serverYaw, realYaw) && Math.abs(serverPitch - realPitch) < 0.75f
                && near(renderHeadYaw, headTarget) && near(renderBodyYaw, bodyTarget)
                && Math.abs(renderPitch - realPitch) < 0.75f;
        if (done) {
            serverYaw = realYaw; serverPitch = realPitch;
            active = false;
        }
    }

    private static float approach(float cur, float target, float frac) {
        float d = wrapDegrees(target - cur);
        if (Math.abs(d) < 0.4f) return target;
        return cur + d * frac;
    }

    private static boolean near(float a, float b) {
        return Math.abs(wrapDegrees(b - a)) < 1.0f;
    }

    private static float wrapDegrees(float v) {
        v %= 360f;
        if (v >= 180f)  v -= 360f;
        if (v < -180f) v += 360f;
        return v;
    }

    public static void applyMoveFix(EventMovementFix e, float serverYaw) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float playerYaw = mc.player.getYRot();

        // Difference between where player is looking and where server thinks
        float yawDiff = (float) Math.toRadians(playerYaw - serverYaw);

        Vec3 input = e.getInputVector();

        double forward = input.z;
        double strafe  = input.x;

        // Rotate movement vector
        double cos = Math.cos(yawDiff);
        double sin = Math.sin(yawDiff);

        double fixedX = strafe * cos - forward * sin;
        double fixedZ = forward * cos + strafe * sin;

        e.setInputVector(new Vec3(fixedX, input.y, fixedZ));
    }

}
