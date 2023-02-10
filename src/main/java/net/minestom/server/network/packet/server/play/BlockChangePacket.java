package net.minestom.server.network.packet.server.play;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.BLOCK_POSITION;
import static net.minestom.server.network.NetworkBuffer.VAR_INT;

public record BlockChangePacket(Point blockPosition, int blockStateId) implements ServerPacket {
    public BlockChangePacket(Point blockPosition, Block block) {
        this(blockPosition, block.stateId());
    }

    public BlockChangePacket(NetworkBuffer reader) {
        this(reader.read(BLOCK_POSITION), reader.read(VAR_INT));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(BLOCK_POSITION, blockPosition);
        writer.write(VAR_INT, blockStateId);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.BLOCK_CHANGE;
    }
}
