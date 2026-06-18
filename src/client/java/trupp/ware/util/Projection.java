package trupp.ware.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * World-space → screen-space projection. The matrices are snapshotted during world rendering
 * (see TruppWareClient's world-render hook + GameRendererMixin) so 2D HUD draws can place text
 * at 3D positions (used by the damage popups).
 */
public class Projection {

    /** Latest projection matrix seen (updated by GameRendererMixin every getProjectionMatrix call). */
    public static final Matrix4f latestProjection = new Matrix4f();

    // Snapshot taken during the world render pass (the correct world matrices for this frame).
    private static final Matrix4f projection = new Matrix4f();
    private static final Matrix4f modelView  = new Matrix4f();
    private static Vec3 cameraPos = Vec3.ZERO;
    private static boolean valid = false;

    /**
     * Build the camera-relative view matrix from the camera's own yaw/pitch (so it doesn't depend
     * on Fabric's WorldRenderContext PoseStack, which can be null under Iris at this phase).
     */
    public static void snapshot(Vec3 cam, float pitch, float yaw) {
        cameraPos = cam;
        modelView.identity()
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw + 180.0f));
        projection.set(latestProjection);
        valid = true;
    }

    /**
     * Projects a world point to GUI-scaled screen coordinates.
     *
     * @return (screenX, screenY, depth) or {@code null} if behind the camera / not ready.
     */
    public static Vector3f worldToScreen(double wx, double wy, double wz, int scaledW, int scaledH) {
        if (!valid) return null;

        Vector4f pos = new Vector4f(
                (float) (wx - cameraPos.x),
                (float) (wy - cameraPos.y),
                (float) (wz - cameraPos.z),
                1.0f);

        modelView.transform(pos);
        projection.transform(pos);

        if (pos.w <= 0.0001f) return null; // behind camera

        float inv = 1.0f / pos.w;
        float ndcX = pos.x * inv;
        float ndcY = pos.y * inv;

        float sx = (ndcX * 0.5f + 0.5f) * scaledW;
        float sy = (1.0f - (ndcY * 0.5f + 0.5f)) * scaledH;
        return new Vector3f(sx, sy, pos.w);
    }
}
