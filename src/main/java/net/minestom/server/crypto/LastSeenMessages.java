package net.minestom.server.crypto;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record LastSeenMessages(List<Entry> entries) implements NetworkBuffer.Writer {
    public LastSeenMessages {
        entries = List.copyOf(entries);
    }

    public LastSeenMessages(NetworkBuffer reader) {
        this(reader.readCollection(Entry::new));
    }

    @Override
    public void write(NetworkBuffer writer) {
    }

    public record Entry(UUID from, MessageSignature lastSignature) implements NetworkBuffer.Writer {
        public Entry(NetworkBuffer reader) {
            this(reader.read(NetworkBuffer.UUID), new MessageSignature(reader));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(NetworkBuffer.UUID, from);
            writer.write(lastSignature);
        }
    }

    public record Update(LastSeenMessages lastSeen, @Nullable Entry lastReceived) implements NetworkBuffer.Writer {
        public Update(NetworkBuffer reader) {
            this(new LastSeenMessages(reader), reader.readOptional(Entry::new));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(lastSeen);
            writer.writeOptional(lastReceived);
        }
    }
}
