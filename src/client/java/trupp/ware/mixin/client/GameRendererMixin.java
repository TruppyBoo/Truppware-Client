package trupp.ware.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import trupp.ware.util.Projection;
import trupp.ware.util.RotationUtil;

/**
 * - Captures the live projection matrix (for world->screen projection).
 * - Redirects the crosshair pick: while a silent rotation is active (Aura/Scaffold), the
 *   {@code hitResult} (what you break/place/attack) is computed from the SERVER aim, so your
 *   manual clicks interact with whatever Aura/Scaffold is looking at — not the block under your
 *   real crosshair. The camera still renders your real view (we restore the rotation after pick).
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getProjectionMatrix", at = @At("RETURN"))
    private void truppware$captureProjection(float fov, CallbackInfoReturnable<Matrix4f> cir) {
        Projection.latestProjection.set(cir.getReturnValue());
    }

    @Unique private float truppware$yaw, truppware$yawO, truppware$pitch, truppware$pitchO;
    @Unique private boolean truppware$picking;

    @Inject(method = "pick", at = @At("HEAD"))
    private void truppware$pickHead(float partialTicks, CallbackInfo ci) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !RotationUtil.active) return;

        truppware$yaw    = p.getYRot();
        truppware$yawO   = p.yRotO;
        truppware$pitch  = p.getXRot();
        truppware$pitchO = p.xRotO;

        p.setYRot(RotationUtil.serverYaw);
        p.yRotO = RotationUtil.serverYaw;
        p.setXRot(RotationUtil.serverPitch);
        p.xRotO = RotationUtil.serverPitch;
        truppware$picking = true;
    }

    @Inject(method = "pick", at = @At("RETURN"))
    private void truppware$pickReturn(float partialTicks, CallbackInfo ci) {
        if (!truppware$picking) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            p.setYRot(truppware$yaw);
            p.yRotO = truppware$yawO;
            p.setXRot(truppware$pitch);
            p.xRotO = truppware$pitchO;
        }
        truppware$picking = false;
    }
}
