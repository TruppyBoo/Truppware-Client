package trupp.ware.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;

@Mixin(Minecraft.class)
public class TickMixin {

    @Inject(at = @At("HEAD"), method = "tick", cancellable = true)
    private void onTickPre(CallbackInfo info) {
        if (TruppWareClient.trupp == null) return;
        EventTick event = new EventTick();
        TruppWareClient.trupp.onEvent(event, Timing.PRE);
        if (event.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(at = @At("RETURN"), method = "tick")
    private void onTickPost(CallbackInfo info) {
        if (TruppWareClient.trupp == null) return;
        TruppWareClient.trupp.onEvent(new EventTick(), Timing.POST);
    }
}