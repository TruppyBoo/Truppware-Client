package trupp.ware.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.COMBAT.Aura;
import trupp.ware.truppware.module.player.Scaffold;
import trupp.ware.util.RotationUtil;

@Mixin(Entity.class)
public class EntityMixin {

    @Unique
    private float truppware$storedYaw;

    @Unique
    private boolean truppware$applying = false;







    @Inject(method = "moveRelative", at = @At("HEAD"))
    private void onMoveRelativeHead(float speed, Vec3 input, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof LocalPlayer player)) return;
        if (input.x == 0 && input.z == 0) return;
        if (truppware$applying) return;

        // Gate on the central 'active' flag so the move-fix keeps running through the smooth-out
        // and eases (serverYaw eases back to real) instead of cutting out and lurching.
        if (!RotationUtil.active) return;

        truppware$storedYaw = player.getYRot();
        truppware$applying  = true;
        player.setYRot(RotationUtil.serverYaw);
    }

    @Inject(method = "moveRelative", at = @At("RETURN"))
    private void onMoveRelativeReturn(float speed, Vec3 input, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof LocalPlayer player)) return;
        if (!truppware$applying) return;

        player.setYRot(truppware$storedYaw);
        truppware$applying = false;
    }
}