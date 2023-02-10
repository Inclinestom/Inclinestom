package net.minestom.server.network.packet.server.play;

import net.kyori.adventure.text.Component;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.minestom.server.network.NetworkBuffer.*;

public record MapDataPacket(int mapId, byte scale, boolean locked,
                            boolean trackingPosition, List<Icon> icons,
                            @Nullable MapDataPacket.ColorContent colorContent) implements ServerPacket {
    public MapDataPacket {
        icons = List.copyOf(icons);
    }

    public MapDataPacket(NetworkBuffer reader) {
        this(read(reader));
    }

    private MapDataPacket(MapDataPacket packet) {
        this(packet.mapId, packet.scale, packet.locked,
                packet.trackingPosition, packet.icons,
                packet.colorContent);
    }

    private static MapDataPacket read(NetworkBuffer reader) {
        var mapId = reader.read(VAR_INT);
        var scale = reader.read(BYTE);
        var locked = reader.read(BOOLEAN);
        var trackingPosition = reader.read(BOOLEAN);
        List<Icon> icons = trackingPosition ? reader.readCollection(Icon::new) : List.of();

        var columns = reader.read(BYTE);
        if (columns <= 0) return new MapDataPacket(mapId, scale, locked, trackingPosition, icons, null);
        byte rows = reader.read(BYTE);
        byte x = reader.read(BYTE);
        byte z = reader.read(BYTE);
        byte[] data = reader.read(BYTE_ARRAY);
        return new MapDataPacket(mapId, scale, locked,
                trackingPosition, icons, new ColorContent(columns, rows, x, z,
                data));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(VAR_INT, mapId);
        writer.write(BYTE, scale);
        writer.write(BOOLEAN, locked);
        writer.write(BOOLEAN, trackingPosition);
        if (trackingPosition) writer.writeCollection(icons);
        if (colorContent != null) {
            writer.write(colorContent);
        } else {
            writer.write(BYTE, (byte) 0);
        }
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.MAP_DATA;
    }

    public record Icon(int type, byte x, byte z, byte direction,
                       @Nullable Component displayName) implements NetworkBuffer.Writer {
        public Icon(NetworkBuffer reader) {
            this(reader.read(VAR_INT), reader.read(BYTE), reader.read(BYTE), reader.read(BYTE),
                    reader.read(BOOLEAN) ? reader.read(COMPONENT) : null);
        }

        public void write(NetworkBuffer writer) {
            writer.write(VAR_INT, type);
            writer.write(BYTE, x);
            writer.write(BYTE, z);
            writer.write(BYTE, direction);
            writer.write(BOOLEAN, displayName != null);
            if (displayName != null) writer.write(COMPONENT, displayName);
        }
    }

    public record ColorContent(byte columns, byte rows, byte x, byte z,
                               byte [] data) implements NetworkBuffer.Writer {
        public ColorContent(NetworkBuffer reader) {
            this(reader.read(BYTE), reader.read(BYTE), reader.read(BYTE), reader.read(BYTE),
                    reader.read(BYTE_ARRAY));
        }

        public void write(NetworkBuffer writer) {
            writer.write(BYTE, columns);
            writer.write(BYTE, rows);
            writer.write(BYTE, x);
            writer.write(BYTE, z);
            writer.write(BYTE_ARRAY, data);
        }
    }
}
