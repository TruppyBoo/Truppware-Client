package trupp.ware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

import java.util.List;
import java.util.Optional;

public class RaycastUtil {

    public static HitResult raycast(Minecraft mc, double reach, float yaw, float pitch) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return null;

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 look   = getLookVec(yaw, pitch);
        Vec3 end    = eyePos.add(look.scale(reach));

        // Block ray first
        HitResult blockHit = mc.level.clip(new ClipContext(
                eyePos, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        // Shorten ray if block is in the way
        Vec3 traceEnd   = end;
        double maxDistSq = reach * reach;

        if (blockHit.getType() != HitResult.Type.MISS) {
            traceEnd  = blockHit.getLocation();
            maxDistSq = eyePos.distanceToSqr(traceEnd);
        }

        // Entity sweep
        AABB sweepBox = player.getBoundingBox()
                .expandTowards(look.scale(reach));

        List<Entity> entities = mc.level.getEntities(
                player, sweepBox,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator()
        );

        EntityHitResult closest     = null;
        double          closestDistSq = maxDistSq;

        for (Entity entity : entities) {
            AABB aabb = entity.getBoundingBox();
            Optional<Vec3> hit = aabb.clip(eyePos, traceEnd);

            if (hit.isEmpty()) {
                // Also check if eye is inside the box
                if (!aabb.contains(eyePos)) continue;
                hit = Optional.of(eyePos);
            }

            double distSq = eyePos.distanceToSqr(hit.get());
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = new EntityHitResult(entity, hit.get());
            }
        }

        return closest != null ? closest : blockHit;
    }

    private static Vec3 getLookVec(float yaw, float pitch) {
        // Standard Minecraft look vector calculation
        float yawRad   = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        float cosPitch = (float) Math.cos(pitchRad);

        return new Vec3(
                -Math.sin(yawRad) * cosPitch,
                -Math.sin(pitchRad),
                Math.cos(yawRad) * cosPitch
        );
    }
}