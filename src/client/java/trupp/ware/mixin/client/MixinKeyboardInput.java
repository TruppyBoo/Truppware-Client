package trupp.ware.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventMovementInput;
import trupp.ware.event.events.Timing;
import trupp.ware.util.DirectionalInput;


@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput {

    @Shadow
    @Final
    private Options options;

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "NEW",
                    target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"
            )
    )
    private Input onCreateInput(Input original) {

        // Convert vanilla input → your directional wrapper
        DirectionalInput directional = new DirectionalInput(
                original.forward(),
                original.backward(),
                original.left(),
                original.right()
        );

        // Fire event
        EventMovementInput event = new EventMovementInput(
                directional,
                original.jump(),
                original.shift()
        );

        TruppWareClient.trupp.onEvent(event, Timing.PRE);

        DirectionalInput d = event.getDirectionalInput();

        // Rebuild Input using record constructor
        return new Input(
                d.isForward(),
                d.isBackward(),
                d.isLeft(),
                d.isRight(),
                event.isJump(),
                event.isSneak(),
                original.sprint() // preserve sprint
        );
    }
}
