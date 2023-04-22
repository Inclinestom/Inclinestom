package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public record TranslateWorldView(WorldView worldView, Area translatedArea, Point translation) implements WorldView {

    public TranslateWorldView(WorldView worldView, Point translation) {
        this(worldView, worldView.area().translate(translation), translation);
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        return worldView.getBlock(x - translation.blockX(), y - translation.blockY(), z - translation.blockZ(), condition);
    }

    @Override
    public @NotNull Area area() {
        return translatedArea;
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        return worldView.getBiome(x - translation.blockX(), y - translation.blockY(), z - translation.blockZ());
    }
}
