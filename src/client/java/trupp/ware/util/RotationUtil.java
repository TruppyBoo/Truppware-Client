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
    public static boolean active;

    private static boolean sending = false;


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
