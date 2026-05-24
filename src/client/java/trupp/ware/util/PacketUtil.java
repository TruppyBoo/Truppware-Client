package trupp.ware.util;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;

public class PacketUtil {

    public static ServerboundMovePlayerPacket modifyRotation(
            ServerboundMovePlayerPacket packet, float yaw, float pitch)
    {
        if(packet instanceof ServerboundMovePlayerPacket.Pos)
            return new ServerboundMovePlayerPacket.PosRot(packet.getX(0), packet.getY(0), packet.getZ(0),
                    yaw, pitch, packet.isOnGround(), packet.horizontalCollision());

        if(packet instanceof ServerboundMovePlayerPacket.StatusOnly)
            return new ServerboundMovePlayerPacket.Rot(yaw, pitch, packet.isOnGround(),
                    packet.horizontalCollision());

        if(packet instanceof ServerboundMovePlayerPacket.PosRot)
            return new ServerboundMovePlayerPacket.PosRot(packet.getX(0), packet.getY(0), packet.getZ(0),
                    yaw, pitch, packet.isOnGround(), packet.horizontalCollision());

        return new ServerboundMovePlayerPacket.Rot(yaw, pitch, packet.isOnGround(),
                packet.horizontalCollision());
    }


}
