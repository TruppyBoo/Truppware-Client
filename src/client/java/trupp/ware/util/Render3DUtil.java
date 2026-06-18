package trupp.ware.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import trupp.ware.event.events.EventWorldRender;

/**
 * Helpers for drawing 3D geometry in the world from an {@link EventWorldRender}.
 *
 * <p>All coordinates passed in are WORLD coordinates; camera-relative conversion is handled here.
 * Lines are drawn with a no-depth-test type so they show THROUGH walls (ESP), falling back to the
 * normal depth-tested type if the custom one can't be built.</p>
 */
public class Render3DUtil {

    private static VertexConsumer lineBuffer(EventWorldRender e) {
        RenderType type = EspRenderType.ready() ? EspRenderType.lines() : RenderTypes.lines();
        return e.getBufferSource().getBuffer(type);
    }

    /**
     * Draws a flat (horizontal) ring on the ground centred at world ({@code cx},{@code cy},{@code cz}).
     *
     * @param color ARGB, e.g. 0xFF00FFFF (include alpha)
     */
    public static void drawCircle(EventWorldRender e, double cx, double cy, double cz,
                                  double radius, int segments, int color) {
        Vec3 cam = e.getCameraPos();
        PoseStack pose = e.getPoseStack();
        VertexConsumer vc = lineBuffer(e);
        Matrix4f matrix = pose.last().pose();

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;

        // Centre the ring relative to the camera.
        float ox = (float) (cx - cam.x);
        float oy = (float) (cy - cam.y);
        float oz = (float) (cz - cam.z);

        // Emit the ring as pairs of vertices (LINES). The normal is the segment direction.
        double prevX = 0, prevZ = 0;
        for (int i = 0; i <= segments; i++) {
            double ang = (Math.PI * 2.0) * (i / (double) segments);
            double x = Math.cos(ang) * radius;
            double z = Math.sin(ang) * radius;

            if (i > 0) {
                double dx = x - prevX, dz = z - prevZ;
                double len = Math.sqrt(dx * dx + dz * dz);
                float nx = len == 0 ? 0 : (float) (dx / len);
                float nz = len == 0 ? 0 : (float) (dz / len);

                // 1.21.11 lines() format also needs a per-vertex LineWidth element.
                vc.addVertex(matrix, ox + (float) prevX, oy, oz + (float) prevZ)
                        .setColor(r, g, b, a).setNormal(nx, 0f, nz).setLineWidth(2.0f);
                vc.addVertex(matrix, ox + (float) x, oy, oz + (float) z)
                        .setColor(r, g, b, a).setNormal(nx, 0f, nz).setLineWidth(2.0f);
            }
            prevX = x;
            prevZ = z;
        }
    }

    /**
     * Draws a circle that always faces the camera (a billboard), centred at world
     * ({@code cx},{@code cy},{@code cz}) — so it looks like a round circle from any angle, not a
     * flat ground ring. Built in the camera's right/up plane.
     *
     * @param color ARGB (include alpha)
     */
    public static void drawBillboardCircle(EventWorldRender e, double cx, double cy, double cz,
                                           double radius, int segments, int color, float lineWidth) {
        Vec3 cam = e.getCameraPos();
        VertexConsumer vc = lineBuffer(e);
        Matrix4f m = e.getPoseStack().last().pose();

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;

        var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        org.joml.Vector3fc up   = camera.upVector();
        org.joml.Vector3fc left = camera.leftVector();
        // right = -left
        float rx = -left.x(), ry = -left.y(), rz = -left.z();
        float ux =  up.x(),   uy =  up.y(),   uz =  up.z();

        float ox = (float) (cx - cam.x), oy = (float) (cy - cam.y), oz = (float) (cz - cam.z);

        float px = 0, py = 0, pz = 0;
        for (int i = 0; i <= segments; i++) {
            double ang = (Math.PI * 2.0) * (i / (double) segments);
            float cs = (float) (Math.cos(ang) * radius);
            float sn = (float) (Math.sin(ang) * radius);
            float x = ox + rx * cs + ux * sn;
            float y = oy + ry * cs + uy * sn;
            float z = oz + rz * cs + uz * sn;
            if (i > 0) edge(vc, m, px, py, pz, x, y, z, r, g, b, a, lineWidth);
            px = x; py = y; pz = z;
        }
    }

    /**
     * Draws the 12-edge outline of an axis-aligned bounding box (world coords).
     *
     * @param color ARGB (include alpha)
     */
    public static void drawBox(EventWorldRender e, AABB box, int color, float lineWidth) {
        Vec3 cam = e.getCameraPos();
        VertexConsumer vc = lineBuffer(e);
        Matrix4f m = e.getPoseStack().last().pose();

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;

        float x0 = (float) (box.minX - cam.x), y0 = (float) (box.minY - cam.y), z0 = (float) (box.minZ - cam.z);
        float x1 = (float) (box.maxX - cam.x), y1 = (float) (box.maxY - cam.y), z1 = (float) (box.maxZ - cam.z);

        // bottom rectangle
        edge(vc, m, x0, y0, z0, x1, y0, z0, r, g, b, a, lineWidth);
        edge(vc, m, x1, y0, z0, x1, y0, z1, r, g, b, a, lineWidth);
        edge(vc, m, x1, y0, z1, x0, y0, z1, r, g, b, a, lineWidth);
        edge(vc, m, x0, y0, z1, x0, y0, z0, r, g, b, a, lineWidth);
        // top rectangle
        edge(vc, m, x0, y1, z0, x1, y1, z0, r, g, b, a, lineWidth);
        edge(vc, m, x1, y1, z0, x1, y1, z1, r, g, b, a, lineWidth);
        edge(vc, m, x1, y1, z1, x0, y1, z1, r, g, b, a, lineWidth);
        edge(vc, m, x0, y1, z1, x0, y1, z0, r, g, b, a, lineWidth);
        // verticals
        edge(vc, m, x0, y0, z0, x0, y1, z0, r, g, b, a, lineWidth);
        edge(vc, m, x1, y0, z0, x1, y1, z0, r, g, b, a, lineWidth);
        edge(vc, m, x1, y0, z1, x1, y1, z1, r, g, b, a, lineWidth);
        edge(vc, m, x0, y0, z1, x0, y1, z1, r, g, b, a, lineWidth);
    }

    /** Draws a single line between two world points. */
    public static void drawLine(EventWorldRender e, double x0, double y0, double z0,
                                double x1, double y1, double z1, int color, float lineWidth) {
        Vec3 cam = e.getCameraPos();
        VertexConsumer vc = lineBuffer(e);
        Matrix4f m = e.getPoseStack().last().pose();
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        edge(vc, m,
                (float) (x0 - cam.x), (float) (y0 - cam.y), (float) (z0 - cam.z),
                (float) (x1 - cam.x), (float) (y1 - cam.y), (float) (z1 - cam.z),
                r, g, b, a, lineWidth);
    }

    private static void edge(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float r, float g, float b, float a, float w) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0f) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        vc.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(w);
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(w);
    }

}
