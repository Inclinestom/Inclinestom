package net.minestom.server.network.packet.client.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

public record ClientSelectTradePacket(int selectedSlot) implements ClientPacket {
    public ClientSelectTradePacket(NetworkBuffer reader) {
        this(reader.read(VAR_INT));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(VAR_INT, selectedSlot);
    }
}
