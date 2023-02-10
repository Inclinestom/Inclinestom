package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.UnknownNullability;

record FilledBlockStorage(Block block) implements BlockStorage {
    @Override
    public Area area() {
        return Area.full();
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        return block;
    }
}
