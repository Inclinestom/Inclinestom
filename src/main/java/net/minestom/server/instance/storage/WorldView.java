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
        return mutable(area, nullPointer());
    }

    static Mutable mutable(Area area, WorldView fallback) {
        return mutable(area, fallback, Block.AIR, Biome.PLAINS);
    }

    /**
     * Creates a mutable world view with ONLY the given area.
     * @param area The area to view.
     * @param fallback The fallback world view to use if the given area is out of bounds.
     * @return A mutable world view.
     */
    static Mutable mutable(Area area, WorldView fallback, Block defaultBlock, Biome defaultBiome) {
        Point min = area.min();
        Point max = area.max();

        // Creating the smallest possible worldview will give us best performance possibility.
        int width = max.blockX() - min.blockX();
        int height = max.blockY() - min.blockY();
        int depth = max.blockZ() - min.blockZ();

        // Section
        if (width <= Instance.SECTION_SIZE &&
            height <= Instance.SECTION_SIZE &&
            depth <= Instance.SECTION_SIZE) {
            Mutable translatedSection = translate(new SectionWorldView(defaultBlock, defaultBiome), min);
            return view(translatedSection, area);
        }

        // Chunk
        int chunkMaxY = Integer.MAX_VALUE / (Instance.SECTION_SIZE * Instance.SECTION_SIZE);
        if (width <= Instance.SECTION_SIZE &&
            height < chunkMaxY &&
            depth <= Instance.SECTION_SIZE) {
            Mutable translatedChunk = translate(new ChunkWorldView(height, defaultBlock, defaultBiome), min);
            return view(translatedChunk, area);
        }

        return view(mutable(fallback), area);
    }

    interface Mutable extends WorldView, Block.Setter, Biome.Setter {
        void mutate(Consumer<Mutator> mutator);
        void clear(Area area);

        interface Mutator extends Block.Setter, Biome.Setter {}
    }

    static Union union() {
        return new UnionWorldView();
    }

    static boolean equals(WorldView previous, WorldView next) {
        if (!Area.equals(previous.area(), next.area())) {
            return false;
        }

        // TODO: Optimize equality
        // Idea: Use the hash of sections of the world view to check for equality.
        //       Ideally these sections would increase exponentially in size.
        //       e.g. first equality hash would be 1 block in size, then 2, 4, 8, 16, 32, 64, 128, 256, 512, etc.
        //       This could be implemented by subdividing, and then creating an iterator that goes over the
        //       subdivisions' block points in some (arbitrary) deterministic order.
        for (Point point : previous.area()) {
            if (!previous.getBlock(point).compare(next.getBlock(point))) {
                return false;
            }
        }
        return true;
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
