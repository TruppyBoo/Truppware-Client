package trupp.ware.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventGlow;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.COMBAT.Aura;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.player.Scaffold;
import trupp.ware.util.RotationUtil;

import java.util.Objects;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> {

    /**
     * True only while a module is actively driving silent rotations
     * (Aura locked on a target, or Scaffold bridging). Outside of that we must
     * leave the player's real rotation alone, otherwise the body looks frozen.
     */
    private static boolean truppware$rotationActive() {
        return RotationUtil.active;
    }


    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;solveBodyRot(Lnet/minecraft/world/entity/LivingEntity;FF)F"))
    private float hookBodyYaw(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != Minecraft.getInstance().player || !truppware$rotationActive()) {
            return original;
        }
        return RotationUtil.renderBodyYaw;
    }

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F"))
    private float hookHeadYaw(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != Minecraft.getInstance().player || !truppware$rotationActive()) {
            return original;
        }
        return RotationUtil.renderHeadYaw;
    }

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F"))
    private float hookPitch(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != Minecraft.getInstance().player || !truppware$rotationActive()) {
            return original;
        }
        return RotationUtil.renderPitch;
    }




    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void forceGlowing(S state, boolean bl, boolean bl2, boolean bl3, CallbackInfoReturnable<RenderType> cir) {

        EventGlow event = new EventGlow(null);
        TruppWareClient.trupp.onEvent(event, Timing.PRE);


        for(Module m : Manager.trupp.modules) {
            if(Objects.equals(m.name, "GlowEsp") && m.toggled) {
                cir.setReturnValue(RenderTypes.outline(
                        ((LivingEntityRenderer<T, S, ?>) (Object) this).getTextureLocation(state)
                ));
            }
        }

    }
}
