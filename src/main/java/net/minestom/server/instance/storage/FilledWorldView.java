package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.UnknownNullability;

record FilledWorldView(Block block, Biome biome) implements WorldView {
    @Override
    public Area area() {
        return Area.full();
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        return block;
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        return biome;
    }
}
