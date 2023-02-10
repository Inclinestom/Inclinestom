package net.minestom.server.network.packet.client.status;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPreplayPacket;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.BYTE;

public record LegacyServerListPingPacket(byte payload) implements ClientPreplayPacket {
    public LegacyServerListPingPacket(NetworkBuffer reader) {
        this(reader.read(BYTE));
    }

    @Override
    public void process(PlayerConnection connection) {

    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(BYTE, payload);
    }
}
