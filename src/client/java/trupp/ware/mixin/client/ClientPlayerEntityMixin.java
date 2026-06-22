package trupp.ware.mixin.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventAttackStrength;
import trupp.ware.event.events.EventKnockback;
import trupp.ware.event.events.EventUpdate;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.player.Scaffold;
import trupp.ware.util.RotationUtil;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin extends AbstractClientPlayer {

    @Shadow
    @Final
    protected Minecraft minecraft;

    public ClientPlayerEntityMixin(ClientLevel clientLevel, com.mojang.authlib.GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }



    @Override
    public void lerpMotion(Vec3 vec) {
        if (vec == null) return;

        EventKnockback kbEvent = new EventKnockback(this, vec.x, vec.y, vec.z);
        TruppWareClient.trupp.onEvent(kbEvent, Timing.PRE);

        if (kbEvent.isCanceled()) return;

        super.lerpMotion(new Vec3(kbEvent.getX(), kbEvent.getY(), kbEvent.getZ()));
    }

    // EventUpdate: fired once per tick from sendPosition() — PRE at HEAD (right before the
    // movement/flying packet is built) and POST at RETURN. Rotations set in PRE are carried by that
    // flying packet and attack/use packets sent in PRE land in the correct order. Use this in combat
    // modules (instead of EventTick) for correct packet timing.
    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void truppware$preMotion(CallbackInfo ci) {
        TruppWareClient.trupp.onEvent(new EventUpdate(), Timing.PRE);
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void truppware$postMotion(CallbackInfo ci) {
        TruppWareClient.trupp.onEvent(new EventUpdate(), Timing.POST);
    }

    @Redirect(
            method = "sendPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F")
    )
    private float modifyYaw(LocalPlayer instance) {
        // Always send the silent yaw while a rotation module is active — INCLUDING while right-clicking.
        // The old right-click exception sent the REAL yaw, but Aura's move-fix rotates your input
        // against serverYaw, so the server then simulated your movement with the wrong yaw and your
        // position desynced -> Grim "Simulation" flag the moment you held right-click. Gate on the
        // central 'active' flag, which stays true through the smooth-out, so disabling a module eases
        // the sent yaw back to real instead of snapping.
        return RotationUtil.active ? RotationUtil.serverYaw : instance.getYRot();
    }

    @Redirect(
            method = "sendPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F")
    )
    private float modifyPitch(LocalPlayer instance) {
        // See modifyYaw: silent pitch always while active, even right-clicking, to avoid the desync.
        return RotationUtil.active ? RotationUtil.serverPitch : instance.getXRot();
    }
}