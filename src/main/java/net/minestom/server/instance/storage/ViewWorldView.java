package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.UnknownNullability;

record ViewWorldView(WorldView worldView, Area area) implements WorldView {
    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        if (!area.contains(x, y, z)) {
            return null;
        }
        return worldView.getBlock(x, y, z, condition);
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        if (!area.contains(x, y, z)) {
            return null;
        }
        return worldView.getBiome(x, y, z);
    }
}
