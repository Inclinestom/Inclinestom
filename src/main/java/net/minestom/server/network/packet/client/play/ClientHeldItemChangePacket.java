package net.minestom.server.network.packet.client.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.SHORT;

public record ClientHeldItemChangePacket(short slot) implements ClientPacket {
    public ClientHeldItemChangePacket(NetworkBuffer reader) {
        this(reader.read(SHORT));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(SHORT, slot);
    }
}
