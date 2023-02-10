package net.minestom.server.network.packet.server.login;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.STRING;
import static net.minestom.server.network.NetworkBuffer.VAR_INT;

public record LoginSuccessPacket(UUID uuid, String username, int properties) implements ServerPacket {
    public LoginSuccessPacket(NetworkBuffer reader) {
        this(reader.read(NetworkBuffer.UUID), reader.read(STRING), reader.read(VAR_INT));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(NetworkBuffer.UUID, uuid);
        writer.write(STRING, username);
        writer.write(VAR_INT, properties);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.LOGIN_SUCCESS;
    }
}
