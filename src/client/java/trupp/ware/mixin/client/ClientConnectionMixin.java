package trupp.ware.mixin.client;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventPacket;
import trupp.ware.event.events.PacketDih;
import trupp.ware.event.events.Timing;


@Mixin(Connection.class)
public abstract class ClientConnectionMixin
        extends SimpleChannelInboundHandler<Packet<?>> {


    @Inject(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V",
            ordinal = 0),
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            cancellable = true)
    private void onChannelRead0(ChannelHandlerContext context, Packet<?> packet,
                                CallbackInfo ci) {

        EventPacket event = new EventPacket(packet, PacketDih.INCOMING);
        TruppWareClient.trupp.onEvent(event, Timing.PRE);


        if (event.isCanceled()) ci.cancel();
    }
}