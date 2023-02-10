package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.UnknownNullability;

class EmptyBlockStorage implements BlockStorage {
    @Override
    public Area area() {
        return Area.empty();
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        return null;
    }
}
