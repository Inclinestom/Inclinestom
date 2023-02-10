package net.minestom.server.crypto;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.BYTE_ARRAY;

public record MessageSignature(byte [] signature) implements NetworkBuffer.Writer {
    public MessageSignature(NetworkBuffer reader) {
        this(reader.read(BYTE_ARRAY));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(BYTE_ARRAY, signature);
    }
}
