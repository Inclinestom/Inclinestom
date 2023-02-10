package net.minestom.server.network.packet.server.play;

import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.*;

public record BlockBreakAnimationPacket(int entityId, Point blockPosition,
                                        byte destroyStage) implements ServerPacket {
    public BlockBreakAnimationPacket(NetworkBuffer reader) {
        this(reader.read(VAR_INT), reader.read(BLOCK_POSITION), reader.read(BYTE));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(VAR_INT, entityId);
        writer.write(BLOCK_POSITION, blockPosition);
        writer.write(BYTE, destroyStage);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.BLOCK_BREAK_ANIMATION;
    }
}