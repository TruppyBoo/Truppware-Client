package trupp.ware.mixin.client;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventMoveVector;
import trupp.ware.event.events.Timing;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void hookMoveFix(CallbackInfo ci) {
        if (moveVector == null || (moveVector.x == 0 && moveVector.y == 0)) return;

        EventMoveVector event = new EventMoveVector(moveVector.x, moveVector.y);
        TruppWareClient.trupp.onEvent(event, Timing.PRE);

        if (event.isModified()) {
            float ns = event.getStrafe();
            float nf = event.getForward();

            moveVector = new Vec2(ns, nf);


        }
    }
}