package trupp.ware.mixin.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventAttackStrength;
import trupp.ware.event.events.EventKnockback;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.player.Scaffold;
import trupp.ware.util.RotationUtil;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin extends AbstractClientPlayer {

    @Shadow
    @Final
    protected Minecraft minecraft;

    public ClientPlayerEntityMixin(ClientLevel clientLevel, com.mojang.authlib.GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }



    @Override
    public void lerpMotion(Vec3 vec) {
        if (vec == null) return;

        EventKnockback kbEvent = new EventKnockback(this, vec.x, vec.y, vec.z);
        TruppWareClient.trupp.onEvent(kbEvent, Timing.PRE);

        if (kbEvent.isCanceled()) return;

        super.lerpMotion(new Vec3(kbEvent.getX(), kbEvent.getY(), kbEvent.getZ()));
    }

    @Redirect(
            method = "sendPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F")
    )
    private float modifyYaw(LocalPlayer instance) {
        if (minecraft.mouseHandler.isRightPressed()) return instance.getYRot();

        for (Module m : Manager.trupp.modules) {
            if ((m.getName().equalsIgnoreCase("Aura") || m.getName().equalsIgnoreCase("Scaffold")) && m.toggled) {
                return RotationUtil.serverYaw;
            }
        }

        return instance.getYRot();
    }

    @Redirect(
            method = "sendPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F")
    )
    private float modifyPitch(LocalPlayer instance) {
        if (minecraft.mouseHandler.isRightPressed()) return instance.getXRot();

        for (Module m : Manager.trupp.modules) {
            if ((m.getName().equalsIgnoreCase("Aura") || m.getName().equalsIgnoreCase("Scaffold")) && m.toggled) {
                return RotationUtil.serverPitch;
            }
        }

        return instance.getXRot();
    }
}