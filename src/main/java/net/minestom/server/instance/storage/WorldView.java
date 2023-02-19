package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A view into world data.
 * This contains lighting, block, and biome data.
 */
public interface WorldView extends Block.Getter, Biome.Getter {

    @NotNull Area area();

    static Mutable inMemory() {
        return new InMemoryWorldView();
    }

    /**
     * Creates a mutable world view with {@link net.minestom.server.instance.Instance#SECTION_SIZE} width, height, and depth.
     */
    static Mutable section(Block defaultBlock, Biome defaultBiome, Point section) {
        return new SectionWorldView(defaultBlock, defaultBiome, section);
    }

    static Mutable section(Point section) {
        return new SectionWorldView(Block.AIR, Biome.PLAINS, section);
    }

    /**
     * Creates a mutable world view with {@link net.minestom.server.instance.Instance#SECTION_SIZE} width, and depth.
     */
    static Mutable chunk(DimensionType dimensionType, Block defaultBlock, Biome defaultBiome, int chunkX, int chunkZ) {
        return new ChunkWorldView(dimensionType, defaultBlock, defaultBiome, chunkX, chunkZ);
    }

    static Mutable chunk(DimensionType dimensionType, int chunkX, int chunkZ) {
        return new ChunkWorldView(dimensionType, Block.AIR, Biome.PLAINS, chunkX, chunkZ);
    }

    static Mutable chunk(int chunkX, int chunkZ) {
        return new ChunkWorldView(DimensionType.OVERWORLD, Block.AIR, Biome.PLAINS, chunkX, chunkZ);
    }

    interface Mutable extends WorldView, Block.Setter, Biome.Setter {
        void mutate(Consumer<Mutator> mutator);
        void clear(Area area);

        interface Mutator extends Block.Setter, Biome.Setter {}
    }

    static Union union() {
        return new UnionWorldView();
    }

    interface Union extends WorldView, Mutable {
        void add(WorldView storage);
        boolean remove(WorldView storage);
    }

    static WorldView filled(Block block, Biome biome) {
        return new FilledWorldView(block, biome);
    }

    static WorldView empty() {
        return new EmptyWorldView();
    }

    static WorldView view(WorldView worldView, Area area) {
        return new ViewWorldView(worldView, area);
    }
}
