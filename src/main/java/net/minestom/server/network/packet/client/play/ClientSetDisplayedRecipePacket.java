package net.minestom.server.network.packet.client.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.STRING;

public record ClientSetDisplayedRecipePacket(String recipeId) implements ClientPacket {
    public ClientSetDisplayedRecipePacket {
        if (recipeId.length() > 256) {
            throw new IllegalArgumentException("'recipeId' cannot be longer than 256 characters.");
        }
    }

    public ClientSetDisplayedRecipePacket(NetworkBuffer reader) {
        this(reader.read(STRING));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(STRING, recipeId);
    }
}