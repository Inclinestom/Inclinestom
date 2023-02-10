package net.minestom.server.network.packet.client.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.BOOLEAN;

public record ClientPlayerPacket(boolean onGround) implements ClientPacket {
    public ClientPlayerPacket(NetworkBuffer reader) {
        this(reader.read(BOOLEAN));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(BOOLEAN, onGround);
    }
}
