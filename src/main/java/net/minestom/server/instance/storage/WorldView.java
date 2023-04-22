package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

/**
 * A view into world data.
 * This contains lighting, block, and biome data.
 */
public interface WorldView extends Block.Getter, Biome.Getter {

    @NotNull Area area();

    static Mutable mutable() {
        return new MutableWorldView(nullPointer());
    }

    static Mutable mutable(WorldView fallback) {
        return new MutableWorldView(fallback);
    }

    static Mutable mutable(Area area) {
        return mutable(nullPointer());
    }

    /**
     * Creates a mutable world view with ONLY the given area.
     * @param area The area to view.
     * @param fallback The fallback world view to use if the given area is out of bounds.
     * @return A mutable world view.
     */
    static Mutable mutable(Area area, WorldView fallback) {
        Point min = area.min();
        Point max = area.max();
        if (max.blockX() - min.blockX() <= Instance.SECTION_SIZE &&
            max.blockY() - min.blockY() <= Instance.SECTION_SIZE &&
            max.blockZ() - min.blockZ() <= Instance.SECTION_SIZE) {
            return WorldView.view(WorldView.translate(WorldView.section(), min), area);
        }
        return WorldView.view(new MutableWorldView(fallback), area);
    }

    /**
     * Creates a mutable world view with {@link net.minestom.server.instance.Instance#SECTION_SIZE} width, height, and depth.
     */
    static Mutable section(Block defaultBlock, Biome defaultBiome) {
        return new SectionWorldView(defaultBlock, defaultBiome);
    }

    static Mutable section() {
        return new SectionWorldView(Block.AIR, Biome.PLAINS);
    }

    static Mutable chunk(DimensionType dimensionType, Block defaultBlock, Biome defaultBiome) {
        return new ChunkWorldView(dimensionType, defaultBlock, defaultBiome);
    }

    static Mutable chunk(DimensionType dimensionType) {
        return new ChunkWorldView(dimensionType, Block.AIR, Biome.PLAINS);
    }

    static Mutable chunk() {
        return new ChunkWorldView(DimensionType.OVERWORLD, Block.AIR, Biome.PLAINS);
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

    interface BlockSupplier {
        Block block(int x, int y, int z);
    }

    interface BiomeSupplier {
        Biome biome(int x, int y, int z);
    }

    static WorldView from(BlockSupplier block, BiomeSupplier biome, Area area) {
        return new WorldView() {
            @Override
            public @NotNull Area area() {
                return area;
            }

            @Override
            public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
                return block.block(x, y, z);
            }

            @Override
            public Biome getBiome(int x, int y, int z) {
                return biome.biome(x, y, z);
            }
        };
    }

    static WorldView filled(Block block, Biome biome) {
        return from((x, y, z) -> block, (x, y, z) -> biome, Area.full());
    }

    static WorldView empty() {
        return filled(Block.AIR, Biome.PLAINS);
    }

    static WorldView block(Block block, Point pos) {
        return from((x, y, z) -> block, (x, y, z) -> {
            throw outOfBounds();
        }, Area.block(pos));
    }

    static WorldView biome(Biome biome, Point pos) {
        return from((x, y, z) -> {
            throw outOfBounds();
        }, (x, y, z) -> biome, Area.fill(pos, pos.add(Instance.BIOME_SIZE)));
    }

    static WorldView nullPointer() {
        return new WorldView() {
            @Override
            public @NotNull Area area() {
                return Area.full();
            }

            @Override
            public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
                throw outOfBounds();
            }

            @Override
            public Biome getBiome(int x, int y, int z) {
                throw outOfBounds();
            }
        };
    }

    static WorldView view(WorldView worldView, Area area) {
        return view(worldView, area, nullPointer());
    }

    static Mutable view(Mutable worldView, Area area) {
        return new MutableViewWorldView(worldView, area);
    }

    static WorldView view(WorldView worldView, Area area, WorldView fallback) {
        return from((x, y, z) -> {
            if (area.contains(x, y, z)) {
                return worldView.getBlock(x, y, z);
            } else {
                return fallback.getBlock(x, y, z);
            }
        }, (x, y, z) -> {
            if (area.contains(x, y, z)) {
                return worldView.getBiome(x, y, z);
            } else {
                return fallback.getBiome(x, y, z);
            }
        }, area);
    }

    static WorldView translate(WorldView worldView, Point translation) {
        return new TranslateWorldView(worldView, translation);
    }

    static Mutable translate(Mutable worldView, Point translation) {
        return new MutableTranslateWorldView(worldView, translation);
    }

    static NullPointerException outOfBounds() {
        return new NullPointerException("Out of bounds");
    }
}
