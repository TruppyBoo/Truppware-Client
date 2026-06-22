package trupp.ware.mixin.client;




import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventPacket;
import trupp.ware.event.events.PacketDih;
import trupp.ware.event.events.Timing;
import trupp.ware.util.RotationUtil;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonNetworkHandlerMixin
        implements ClientCommonPacketListener
{
    @WrapOperation(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"),
            method = "send(Lnet/minecraft/network/protocol/Packet;)V")
    private void wrapSendPacket(Connection connection, Packet<?> packet,
                                Operation<Void> original)
    {
        // Keep the use-item packet's rotation in sync with the silent rotation we send in the movement
        // packets. Vanilla builds it from the REAL look, so while a rotation module is active it no
        // longer matched the tick rotation -> Grim BadPacketsJ ("rotation in use item packet did not
        // match tick rotation"). Rebuild it with the server rotation so they agree.
        if (RotationUtil.active && packet instanceof ServerboundUseItemPacket use) {
            packet = new ServerboundUseItemPacket(use.getHand(), use.getSequence(),
                    RotationUtil.serverYaw, RotationUtil.serverPitch);
        }

        EventPacket event = new EventPacket(packet, PacketDih.OUTGOING);
        TruppWareClient.trupp.onEvent(event, Timing.PRE);

        if(!event.isCanceled())
            original.call(connection, event.getPacket());
    }
}