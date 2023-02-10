package net.minestom.server.network.packet.server.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import net.minestom.server.world.Difficulty;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.BOOLEAN;

public record ServerDifficultyPacket(Difficulty difficulty, boolean locked) implements ServerPacket {
    public ServerDifficultyPacket(NetworkBuffer reader) {
        this(reader.readEnum(Difficulty.class), reader.read(BOOLEAN));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.writeEnum(Difficulty.class, difficulty);
        writer.write(BOOLEAN, locked);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.SERVER_DIFFICULTY;
    }
}
