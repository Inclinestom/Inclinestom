package net.minestom.server.network.packet.client.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.LONG;

public record ClientKeepAlivePacket(long id) implements ClientPacket {
    public ClientKeepAlivePacket(NetworkBuffer reader) {
        this(reader.read(LONG));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(LONG, id);
    }
}
