package trupp.ware.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventAttackStrength;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.COMBAT.Reach;
import trupp.ware.truppware.module.COMBAT.SpearFly;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.COMBAT.Aura;

@Mixin(Player.class)
public abstract class MixinPlayer {


//    @ModifyReturnValue(method = "entityInteractionRange", at = @At("RETURN"))
//    private double hookEntityInteractionRange(double original) {
//        if ((Object) this == Minecraft.getInstance().player && ModuleReach.INSTANCE.getRunning()) {
//            return ModuleReach.INSTANCE.getEntity().getInteractionRange$liquidbounce();
//        }
//
//        return original;
//    }



    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
    private void truppware$forceFullStrength(float partialTicks, CallbackInfoReturnable<Float> cir) {
        EventAttackStrength event = new EventAttackStrength(cir.getReturnValueF());
        TruppWareClient.trupp.onEvent(event, Timing.PRE);
        if (event.canceled) {
            cir.setReturnValue(event.getStrength());
        }
    }

    @ModifyReturnValue(method = "entityInteractionRange", at = @At("RETURN"))
    private double hookEntityInteractionRange(double original) {
        if ((Object) this != Minecraft.getInstance().player) return original;

        for (Module m : Manager.trupp.modules) {
            if (m instanceof Reach reach && reach.toggled) {
                return Reach.currentReach;
            }
        }

        return original;
    }

    @ModifyExpressionValue(method = {"causeExtraKnockback",
            "doSweepAttack"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float hookFixRotation(float original) {
        // Only modify for the local player
        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }

        // Check module manager for Aura
        for (Module m : Manager.trupp.modules) {
            if (m instanceof Aura aura && aura.toggled && Aura.yaw != 0) {
                return Aura.yaw; // use Aura's calculated yaw
            }
        }

        return original;
    }



}
