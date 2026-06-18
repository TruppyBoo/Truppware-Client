package trupp.ware.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventNoSlow;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {





    @WrapOperation(
            method = "modifyInput",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec2;scale(F)Lnet/minecraft/world/phys/Vec2;",
                    ordinal = 1)
    )
    private Vec2 hookCustomMultiplier(Vec2 instance, float value, Operation<Vec2> original) {

        boolean noslowEnabled = false;
        for (Module m : Manager.trupp.modules) {
            if (m.name.equals("Noslow") && m.toggled) {
                noslowEnabled = true;
                break;
            }
        }

        if (!noslowEnabled) {
            return original.call(instance, value);
        }


        EventNoSlow event = new EventNoSlow(instance.x, instance.y);
        TruppWareClient.trupp.onEvent(event, Timing.PRE);

        return new Vec2(
                instance.x * event.getX(),
                instance.y * event.getY()
        );
    }
}