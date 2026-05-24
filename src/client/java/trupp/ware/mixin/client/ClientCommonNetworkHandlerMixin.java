package trupp.ware.mixin.client;




import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventPacket;
import trupp.ware.event.events.PacketDih;
import trupp.ware.event.events.Timing;

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


        EventPacket event = new EventPacket(packet, PacketDih.OUTGOING);
        TruppWareClient.trupp.onEvent(event, Timing.PRE);

        if(!event.isCanceled())
            original.call(connection, event.getPacket());
    }
}