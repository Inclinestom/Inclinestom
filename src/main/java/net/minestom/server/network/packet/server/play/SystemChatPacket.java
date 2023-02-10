package net.minestom.server.network.packet.server.play;

import net.kyori.adventure.text.Component;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ComponentHoldingServerPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

import static net.minestom.server.network.NetworkBuffer.BOOLEAN;
import static net.minestom.server.network.NetworkBuffer.COMPONENT;

public record SystemChatPacket(Component message, boolean overlay) implements ComponentHoldingServerPacket {
    public SystemChatPacket(NetworkBuffer reader) {
        this(reader.read(COMPONENT), reader.read(BOOLEAN));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(COMPONENT, message);
        writer.write(BOOLEAN, overlay);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.SYSTEM_CHAT;
    }

    @Override
    public Collection<Component> components() {
        return List.of(message);
    }

    @Override
    public ServerPacket copyWithOperator(UnaryOperator<Component> operator) {
        return new SystemChatPacket(operator.apply(message), overlay);
    }
}
