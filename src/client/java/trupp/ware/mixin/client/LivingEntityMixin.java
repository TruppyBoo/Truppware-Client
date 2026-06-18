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
import trupp.ware.event.events.EventTick;
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
        return RotationUtil.active ? RotationUtil.serverYaw : instance.getYRot();
    }

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void truppware$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        EventSwingSpeed event = new EventSwingSpeed(cir.getReturnValueI());
        TruppWareClient.trupp.onEvent(event, Timing.PRE);
        if (event.canceled) {
            cir.setReturnValue(event.getDuration());
        }
    }

    // NOTE: we intentionally do NOT force yHeadRot/yBodyRot here anymore. Writing the real entity
    // fields left the body stuck at the aim yaw, so on release vanilla snapped it back to the
    // movement direction. The visible model is driven entirely (and smoothly) by the render-state
    // override in LivingEntityRendererMixin, gated on RotationUtil.active.
}