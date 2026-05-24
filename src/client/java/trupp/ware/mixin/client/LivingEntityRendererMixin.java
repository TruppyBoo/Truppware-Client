package trupp.ware.mixin.client;

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
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;

import java.util.Objects;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> {

    /**
     * Forces all entities to glow like spectral arrows.
     */
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
