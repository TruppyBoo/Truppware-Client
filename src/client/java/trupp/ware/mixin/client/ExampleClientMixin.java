package trupp.ware.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import trupp.ware.TruppWareClient;
import trupp.ware.config.ConfigManager;
import trupp.ware.event.events.EventIntialize;
import trupp.ware.event.events.EventUseItemCooldown;
import trupp.ware.event.events.Timing;

@Mixin(Minecraft.class)
public class ExampleClientMixin {


	@Shadow
	private int rightClickDelay;

	@Inject(at = @At("HEAD"), method = "run")
	private void init(CallbackInfo info) {
		EventIntialize event = new EventIntialize();
		TruppWareClient.trupp.onEvent(event, Timing.PRE);
	}

	@Inject(method = "startUseItem", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", shift = At.Shift.AFTER))
	private void hookItemUseCooldown(CallbackInfo callbackInfo) {

		EventUseItemCooldown event = new EventUseItemCooldown(rightClickDelay);
		TruppWareClient.trupp.onEvent(event, Timing.PRE);
		rightClickDelay = event.getCooldown();
	}


}