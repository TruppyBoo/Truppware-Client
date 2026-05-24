package trupp.ware.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventIntialize;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;

@Mixin(Minecraft.class)
public class TickMixin {

	@Inject(at = @At("HEAD"), method = "tick", cancellable = true)
	private void init(CallbackInfo info) {


		EventTick event = new EventTick();
		if(event.isCanceled()){
			info.cancel();
		}
		TruppWareClient.trupp.onEvent(event, Timing.PRE);


	}
}