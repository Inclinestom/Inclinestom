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

public record ActionBarPacket(Component text) implements ComponentHoldingServerPacket {
    public ActionBarPacket(NetworkBuffer reader) {
        this(reader.read(COMPONENT));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(COMPONENT, text);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.ACTION_BAR;
    }

    @Override
    public Collection<Component> components() {
        return List.of(this.text);
    }

    @Override
    public ServerPacket copyWithOperator(UnaryOperator<Component> operator) {
        return new ActionBarPacket(operator.apply(this.text));
    }
}
