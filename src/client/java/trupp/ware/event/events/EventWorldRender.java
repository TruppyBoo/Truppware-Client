package trupp.ware.event.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import trupp.ware.event.Event;

/**
 * Fired once per frame during world rendering (after entities are drawn), so modules can draw
 * 3D geometry into the world — rings, tracers, boxes, etc.
 *
 * <p>All vertex positions must be CAMERA-RELATIVE (subtract {@link #getCameraPos()} from world
 * coordinates). Use the {@link trupp.ware.util.Render3DUtil} helpers which handle this for you.</p>
 */
public class EventWorldRender extends Event<Void> {

    private final PoseStack poseStack;
    private final MultiBufferSource bufferSource;
    private final Vec3 cameraPos;
    private final float partialTick;

    public EventWorldRender(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPos, float partialTick) {
        super("WorldRender");
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.cameraPos = cameraPos;
        this.partialTick = partialTick;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public MultiBufferSource getBufferSource() {
        return bufferSource;
    }

    public Vec3 getCameraPos() {
        return cameraPos;
    }

    public float getPartialTick() {
        return partialTick;
    }
}
