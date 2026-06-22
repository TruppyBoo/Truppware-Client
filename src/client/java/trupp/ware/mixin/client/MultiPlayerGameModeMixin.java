package trupp.ware.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventAttack;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IMultiPlayerGameModeExt;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin implements IMultiPlayerGameModeExt {

    /** Flush a pending held-slot change as its own packet (see {@link IMultiPlayerGameModeExt}). */
    @Invoker("ensureHasSentCarriedItem")
    public abstract void truppware$ensureCarriedItem();

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Player player, Entity entity, CallbackInfo ci) {

        EventAttack event = new EventAttack(player, entity);
        TruppWareClient.trupp.onEvent(event, Timing.PRE);


        if (event.isCanceled()) {
            ci.cancel();
        }
    }
}
