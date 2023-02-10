package net.minestom.server.network.packet.client.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.STRING;
import static net.minestom.server.network.NetworkBuffer.VAR_INT;

public record ClientTabCompletePacket(int transactionId, String text) implements ClientPacket {
    public ClientTabCompletePacket(NetworkBuffer reader) {
        this(reader.read(VAR_INT), reader.read(STRING));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(VAR_INT, transactionId);
        writer.write(STRING, text);
    }
}
