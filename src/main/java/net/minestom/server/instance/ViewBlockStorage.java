package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.UnknownNullability;

record ViewBlockStorage(BlockStorage blockStorage, Area area) implements BlockStorage {
    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        if (!area.contains(x, y, z)) {
            return null;
        }
        return blockStorage.getBlock(x, y, z, condition);
    }
}
