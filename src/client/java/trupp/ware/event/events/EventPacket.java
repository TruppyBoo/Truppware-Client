package trupp.ware.event.events;


import net.minecraft.network.protocol.Packet;
import trupp.ware.event.Event;

public class EventPacket extends Event {


    private final Packet<?> packet;
    private final PacketDih packetDih;

    public EventPacket(Packet<?> packet, PacketDih packetDih) {
        super("Packetevent");
        this.packet = packet;
        this.packetDih = packetDih;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public PacketDih getPacketDih() {
        return packetDih;
    }
}
