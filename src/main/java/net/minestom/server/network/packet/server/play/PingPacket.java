package net.minestom.server.network.packet.server.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.INT;

public record PingPacket(int id) implements ServerPacket {
    public PingPacket(NetworkBuffer reader) {
        this(reader.read(INT));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(INT, id);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.PING;
    }
}
