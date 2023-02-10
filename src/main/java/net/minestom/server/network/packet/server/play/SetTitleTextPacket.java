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

import static net.minestom.server.network.NetworkBuffer.COMPONENT;

public record SetTitleTextPacket(Component title) implements ComponentHoldingServerPacket {
    public SetTitleTextPacket(NetworkBuffer reader) {
        this(reader.read(COMPONENT));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(COMPONENT, title);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.SET_TITLE_TEXT;
    }

    @Override
    public Collection<Component> components() {
        return List.of(this.title);
    }

    @Override
    public ServerPacket copyWithOperator(UnaryOperator<Component> operator) {
        return new SetTitleTextPacket(operator.apply(this.title));
    }
}
