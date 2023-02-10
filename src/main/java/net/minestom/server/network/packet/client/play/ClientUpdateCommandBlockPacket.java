package net.minestom.server.network.packet.client.play;

import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.*;

public record ClientUpdateCommandBlockPacket(Point blockPosition, String command,
                                             Mode mode, byte flags) implements ClientPacket {
    public ClientUpdateCommandBlockPacket(NetworkBuffer reader) {
        this(reader.read(BLOCK_POSITION), reader.read(STRING),
                Mode.values()[reader.read(VAR_INT)], reader.read(BYTE));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(BLOCK_POSITION, blockPosition);
        writer.write(STRING, command);
        writer.write(VAR_INT, mode.ordinal());
        writer.write(BYTE, flags);
    }

    public enum Mode {
        SEQUENCE, AUTO, REDSTONE
    }
}
