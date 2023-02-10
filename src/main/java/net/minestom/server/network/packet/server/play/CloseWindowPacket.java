package net.minestom.server.network.packet.server.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.BYTE;

public record CloseWindowPacket(byte windowId) implements ServerPacket {
    public CloseWindowPacket(NetworkBuffer reader) {
        this(reader.read(BYTE));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(BYTE, windowId);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.CLOSE_WINDOW;
    }
}
