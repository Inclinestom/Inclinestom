package net.minestom.server.network.packet.client.play;

import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.ITEM;
import static net.minestom.server.network.NetworkBuffer.SHORT;

public record ClientCreativeInventoryActionPacket(short slot, ItemStack item) implements ClientPacket {
    public ClientCreativeInventoryActionPacket(NetworkBuffer reader) {
        this(reader.read(SHORT), reader.read(ITEM));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(SHORT, slot);
        writer.write(ITEM, item);
    }
}
