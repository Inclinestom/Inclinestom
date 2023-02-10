package net.minestom.server.network.packet.client.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.INT;

public record ClientPongPacket(int id) implements ClientPacket {
    public ClientPongPacket(NetworkBuffer reader) {
        this(reader.read(INT));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(INT, id);
    }
}
