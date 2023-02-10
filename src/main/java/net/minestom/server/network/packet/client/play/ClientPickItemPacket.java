package net.minestom.server.network.packet.client.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

public record ClientPickItemPacket(int slot) implements ClientPacket {
    public ClientPickItemPacket(NetworkBuffer reader) {
        this(reader.read(VAR_INT));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(VAR_INT, slot);
    }
}
