package net.minestom.server.network.packet.client.play;

import net.minestom.server.crypto.LastSeenMessages;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

public record ClientChatAckPacket(LastSeenMessages.Update update) implements ClientPacket {
    public ClientChatAckPacket(NetworkBuffer reader) {
        this(new LastSeenMessages.Update(reader));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(update);
    }
}
