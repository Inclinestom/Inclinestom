package net.minestom.server.network.packet.server.play;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Metadata;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ComponentHoldingServerPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import static net.minestom.server.network.NetworkBuffer.BYTE;
import static net.minestom.server.network.NetworkBuffer.VAR_INT;

public record EntityMetaDataPacket(int entityId,
                                   Map<Integer, Metadata.Entry<?>> entries) implements ComponentHoldingServerPacket {
    public EntityMetaDataPacket {
        entries = Map.copyOf(entries);
    }

    public EntityMetaDataPacket(NetworkBuffer reader) {
        this(reader.read(VAR_INT), readEntries(reader));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(VAR_INT, entityId);
        for (var entry : entries.entrySet()) {
            writer.write(BYTE, entry.getKey().byteValue());
            writer.write(entry.getValue());
        }
        writer.write(BYTE, (byte) 0xFF); // End
    }

    private static Map<Integer, Metadata.Entry<?>> readEntries(NetworkBuffer reader) {
        Map<Integer, Metadata.Entry<?>> entries = new HashMap<>();
        while (true) {
            final byte index = reader.read(BYTE);
            if (index == (byte) 0xFF) { // reached the end
                break;
            }
            final int type = reader.read(VAR_INT);
            entries.put((int) index, Metadata.Entry.read(type, reader));
        }
        return entries;
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.ENTITY_METADATA;
    }

    @Override
    public Collection<Component> components() {
        return this.entries.values()
                .stream()
                .map(Metadata.Entry::value)
                .filter(entry -> entry instanceof Component)
                .map(entry -> (Component) entry)
                .toList();
    }

    @Override
    public ServerPacket copyWithOperator(UnaryOperator<Component> operator) {
        final var entries = new HashMap<Integer, Metadata.Entry<?>>();

        this.entries.forEach((key, value) -> {
            final var v = value.value();

            entries.put(key, v instanceof Component c ? Metadata.OptChat(operator.apply(c)) : value);
        });

        return new EntityMetaDataPacket(this.entityId, entries);
    }
}
