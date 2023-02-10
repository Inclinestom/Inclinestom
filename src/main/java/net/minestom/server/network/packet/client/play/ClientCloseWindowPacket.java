package net.minestom.server.network.packet.client.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.BYTE;

public record ClientCloseWindowPacket(byte windowId) implements ClientPacket {
    public ClientCloseWindowPacket(NetworkBuffer reader) {
        this(reader.read(BYTE));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(BYTE, windowId);
    }
}
