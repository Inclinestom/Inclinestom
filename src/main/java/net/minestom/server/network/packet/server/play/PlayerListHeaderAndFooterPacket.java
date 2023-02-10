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

public record PlayerListHeaderAndFooterPacket(Component header,
                                              Component footer) implements ComponentHoldingServerPacket {
    public PlayerListHeaderAndFooterPacket(NetworkBuffer reader) {
        this(reader.read(COMPONENT), reader.read(COMPONENT));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(COMPONENT, header);
        writer.write(COMPONENT, footer);
    }

    @Override
    public Collection<Component> components() {
        return List.of(header, footer);
    }

    @Override
    public ServerPacket copyWithOperator(UnaryOperator<Component> operator) {
        return new PlayerListHeaderAndFooterPacket(operator.apply(header), operator.apply(footer));
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.PLAYER_LIST_HEADER_AND_FOOTER;
    }
}
