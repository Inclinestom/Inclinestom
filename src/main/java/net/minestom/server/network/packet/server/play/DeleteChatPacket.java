package net.minestom.server.network.packet.server.play;

import net.minestom.server.crypto.MessageSignature;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

public record DeleteChatPacket(MessageSignature signature) implements ServerPacket {
    public DeleteChatPacket(NetworkBuffer reader) {
        this(new MessageSignature(reader));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(signature);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.DELETE_CHAT_MESSAGE;
    }
}
