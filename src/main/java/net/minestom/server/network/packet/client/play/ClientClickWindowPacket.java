package net.minestom.server.network.packet.client.play;

import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.minestom.server.network.NetworkBuffer.*;

public record ClientClickWindowPacket(byte windowId, int stateId,
                                      short slot, byte button, ClickType clickType,
                                      List<ChangedSlot> changedSlots,
                                      ItemStack clickedItem) implements ClientPacket {
    public ClientClickWindowPacket {
        changedSlots = List.copyOf(changedSlots);
    }

    public ClientClickWindowPacket(NetworkBuffer reader) {
        this(reader.read(BYTE), reader.read(VAR_INT),
                reader.read(SHORT), reader.read(BYTE), reader.readEnum(ClickType.class),
                reader.readCollection(ChangedSlot::new), reader.read(ITEM));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(BYTE, windowId);
        writer.write(VAR_INT, stateId);
        writer.write(SHORT, slot);
        writer.write(BYTE, button);
        writer.write(VAR_INT, clickType.ordinal());
        writer.writeCollection(changedSlots);
        writer.write(ITEM, clickedItem);
    }

    public record ChangedSlot(short slot, ItemStack item) implements NetworkBuffer.Writer {
        public ChangedSlot(NetworkBuffer reader) {
            this(reader.read(SHORT), reader.read(ITEM));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(SHORT, slot);
            writer.write(ITEM, item);
        }
    }

    public enum ClickType {
        PICKUP,
        QUICK_MOVE,
        SWAP,
        CLONE,
        THROW,
        QUICK_CRAFT,
        PICKUP_ALL
    }
}
