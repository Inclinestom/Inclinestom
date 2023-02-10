package net.minestom.server.network.packet.server.login;

import net.kyori.adventure.text.Component;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ComponentHoldingServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

import static net.minestom.server.network.NetworkBuffer.COMPONENT;

public record LoginDisconnectPacket(Component kickMessage) implements ComponentHoldingServerPacket {
    public LoginDisconnectPacket(NetworkBuffer reader) {
        this(reader.read(COMPONENT));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(COMPONENT, kickMessage);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.LOGIN_DISCONNECT;
    }

    @Override
    public Collection<Component> components() {
        return List.of(this.kickMessage);
    }

    @Override
    public LoginDisconnectPacket copyWithOperator(UnaryOperator<Component> operator) {
        return new LoginDisconnectPacket(operator.apply(this.kickMessage));
    }
}
