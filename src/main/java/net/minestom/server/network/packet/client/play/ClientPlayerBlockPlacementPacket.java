package net.minestom.server.network.packet.client.play;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.*;

public record ClientPlayerBlockPlacementPacket(Player.Hand hand, Point blockPosition,
                                               BlockFace blockFace,
                                               float cursorPositionX, float cursorPositionY, float cursorPositionZ,
                                               boolean insideBlock, int sequence) implements ClientPacket {
    public ClientPlayerBlockPlacementPacket(NetworkBuffer reader) {
        this(reader.readEnum(Player.Hand.class), reader.read(BLOCK_POSITION),
                reader.readEnum(BlockFace.class),
                reader.read(FLOAT), reader.read(FLOAT), reader.read(FLOAT),
                reader.read(BOOLEAN), reader.read(VAR_INT));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.writeEnum(Player.Hand.class, hand);
        writer.write(BLOCK_POSITION, blockPosition);
        writer.writeEnum(BlockFace.class, blockFace);
        writer.write(FLOAT, cursorPositionX);
        writer.write(FLOAT, cursorPositionY);
        writer.write(FLOAT, cursorPositionZ);
        writer.write(BOOLEAN, insideBlock);
        writer.write(VAR_INT, sequence);
    }
}
