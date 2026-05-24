package trupp.ware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import trupp.ware.event.events.EventMovementFix;
import trupp.ware.event.events.EventMovementInput;

public class MoveFixUtil {

    public static void applyMoveFix(EventMovementFix e, float serverYaw) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float playerYaw = mc.player.getYRot();

        // Proper wrapped delta (VERY important for Grim)
        float deltaYaw = playerYaw - serverYaw;
        deltaYaw = (deltaYaw % 360.0F + 540.0F) % 360.0F - 180.0F;

        float rad = (float) Math.toRadians(deltaYaw);

        Vec3 input = e.getInputVector();

        double strafe  = input.x;
        double forward = input.z;

        // If no movement, don't touch (Grim check)
        if (strafe == 0 && forward == 0) return;

        float sin = (float) Math.sin(rad);
        float cos = (float) Math.cos(rad);

        // Vanilla-style rotation (matches moveRelative)
        double fixedX = strafe * cos - forward * sin;
        double fixedZ = forward * cos + strafe * sin;

        // DO NOT normalize (Grim checks this)
        e.setInputVector(new Vec3(fixedX, input.y, fixedZ));
    }
}