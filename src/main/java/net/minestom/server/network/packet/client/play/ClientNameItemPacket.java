package net.minestom.server.network.packet.client.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.STRING;

public record ClientNameItemPacket(String itemName) implements ClientPacket {
    public ClientNameItemPacket {
        if (itemName.length() > Short.MAX_VALUE) {
            throw new IllegalArgumentException("ItemStack name cannot be longer than Short.MAX_VALUE characters!");
        }
    }

    public ClientNameItemPacket(NetworkBuffer reader) {
        this(reader.read(STRING));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(STRING, itemName);
    }
}
