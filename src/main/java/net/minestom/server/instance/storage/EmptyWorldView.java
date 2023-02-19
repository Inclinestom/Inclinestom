package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.UnknownNullability;

class EmptyWorldView implements WorldView {
    @Override
    public Area area() {
        return Area.empty();
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        return null;
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        return null;
    }
}
