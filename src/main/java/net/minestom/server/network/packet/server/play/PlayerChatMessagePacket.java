package net.minestom.server.network.packet.server.play;

import net.kyori.adventure.text.Component;
import net.minestom.server.crypto.MessageSignature;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ComponentHoldingServerPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static net.minestom.server.network.NetworkBuffer.*;

/**
 * Represents an outgoing chat message packet.
 */
public record PlayerChatMessagePacket(Component signedContent, @Nullable Component unsignedContent,
                                      int type, UUID uuid,
                                      Component displayName, @Nullable Component teamDisplayName,
                                      MessageSignature signature) implements ComponentHoldingServerPacket {
    public PlayerChatMessagePacket(NetworkBuffer reader) {
        this(reader.read(COMPONENT), reader.readOptional(COMPONENT),
                reader.read(VAR_INT), reader.read(NetworkBuffer.UUID),
                reader.read(COMPONENT), reader.readOptional(COMPONENT),
                new MessageSignature(reader));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(COMPONENT, signedContent);
        writer.writeOptional(COMPONENT, unsignedContent);
        writer.write(VAR_INT, type);
        writer.write(UUID, uuid);
        writer.write(COMPONENT, displayName);
        writer.writeOptional(COMPONENT, teamDisplayName);
        writer.write(signature);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.PLAYER_CHAT;
    }

    @Override
    public Collection<Component> components() {
        return List.of(signedContent);
    }

    @Override
    public ServerPacket copyWithOperator(UnaryOperator<Component> operator) {
        return new PlayerChatMessagePacket(signedContent, unsignedContent, type,
                uuid, displayName, teamDisplayName, signature);
    }
}
