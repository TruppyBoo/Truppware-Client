package trupp.ware.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventSwingSpeed;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.COMBAT.Aura;
import trupp.ware.truppware.module.player.Scaffold;
import trupp.ware.util.RotationUtil;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Shadow
    public float yHeadRot;

    @Shadow
    public float yHeadRotO;

    @Shadow
    public float yBodyRot;

    @Shadow
    public float yBodyRotO;

    @Redirect(
            method = "jumpFromGround",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F")
    )
    private float hookJumpYaw(LivingEntity instance) {
        if (!(instance instanceof LocalPlayer)) return instance.getYRot();


        if (Aura.isEnabledStatic() && Aura.targetInRange) return RotationUtil.serverYaw;
        if (Scaffold.isEnabledStatic() && Scaffold.rotating) return RotationUtil.serverYaw;

        return instance.getYRot();
    }

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void truppware$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        EventSwingSpeed event = new EventSwingSpeed(cir.getReturnValueI());
        TruppWareClient.trupp.onEvent(event, Timing.PRE);
        if (event.canceled) {
            cir.setReturnValue(event.getDuration());
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void hookHeadRot(CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof LocalPlayer)) return;


        if (Aura.isEnabledStatic() && Aura.targetInRange) {
            yHeadRot  = Aura.yaw;
            yHeadRotO = Aura.yaw;
            yBodyRot = Aura.yaw;
            yBodyRotO = Aura.yaw;
            return;
        }


        if (Scaffold.isEnabledStatic()) {
            yHeadRot  = RotationUtil.serverYaw;
            yHeadRotO = RotationUtil.serverYaw;
            yBodyRot = RotationUtil.serverYaw;
            yBodyRotO = RotationUtil.serverYaw;
        }
    }
}