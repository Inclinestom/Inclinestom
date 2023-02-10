package net.minestom.server.network.packet.client.play;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.DOUBLE;
import static net.minestom.server.network.NetworkBuffer.FLOAT;

public record ClientVehicleMovePacket(Pos position) implements ClientPacket {
    public ClientVehicleMovePacket(NetworkBuffer reader) {
        this(new Pos(reader.read(DOUBLE), reader.read(DOUBLE), reader.read(DOUBLE),
                reader.read(FLOAT), reader.read(FLOAT)));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(DOUBLE, position.x());
        writer.write(DOUBLE, position.y());
        writer.write(DOUBLE, position.z());
        writer.write(FLOAT, position.yaw());
        writer.write(FLOAT, position.pitch());
    }
}
