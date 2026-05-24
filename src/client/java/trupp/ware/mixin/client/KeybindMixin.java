package trupp.ware.mixin.client;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent; // This is the key!
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventIntialize;
import trupp.ware.event.events.Timing;

@Mixin(KeyboardHandler.class)
public class KeybindMixin {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyPress(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        if (TruppWareClient.trupp != null) {
            // Use keyEvent.key() to get the integer key code

            TruppWareClient.trupp.onKey(keyEvent.key(), action);
        }
    }
}