package net.minestom.server.network.packet.server.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.BOOLEAN;

public record ClearTitlesPacket(boolean reset) implements ServerPacket {
    public ClearTitlesPacket(NetworkBuffer reader) {
        this(reader.read(BOOLEAN));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(BOOLEAN, reset);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.CLEAR_TITLES;
    }
}
