package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

/**
 * A view into world data.
 * This contains lighting, block, and biome data.
 */
public interface WorldView extends Block.Getter, Biome.Getter {

    /**
     * @return The area where data may be read from within this worldview.
     */
    @NotNull Area area();

    /**
     * Gets the block from this worldview at the given position, returning null if not found, and throwing an exception
     * if the position is out of bounds.
     * @param x The x position.
     * @param y The y position.
     * @param z The z position.
     * @return The block at the given position, or null if not found.
     * @throws IndexOutOfBoundsException If the position is out of bounds.
     */
    @Override
    @Nullable Block getBlock(int x, int y, int z, Condition condition);

    /**
     * Gets the biome from this worldview at the given position, returning null if not found, and throwing an exception
     * if the position is out of bounds.
     * @param x The x position.
     * @param y The y position.
     * @param z The z position.
     * @return The biome at the given position, or null if not found.
     * @throws IndexOutOfBoundsException If the position is out of bounds.
     */
    @Override
    @Nullable Biome getBiome(int x, int y, int z);

    static Mutable mutable() {
        return new MutableWorldView();
    }

    static Mutable mutable(Area area) {
        return mutable(area, Block.AIR, Biome.PLAINS);
    }

    /**
     * Creates a mutable world view with ONLY the given area.
     * @param area The area to view.
     * @return A mutable world view.
     */
    static Mutable mutable(Area area, Block defaultBlock, Biome defaultBiome) {
        Point min = area.min();
        Point max = area.max();

        // Create the smallest possible worldview implementation
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

        return view(mutable(), area);
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
        @Nullable Block block(int x, int y, int z);
    }

    interface BiomeSupplier {
        @Nullable Biome biome(int x, int y, int z);
    }

    static WorldView from(BlockSupplier block, BiomeSupplier biome, Area area) {
        return new WorldView() {
            @Override
            public @NotNull Area area() {
                return area;
            }

            @Override
            public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
                if (!area.contains(x, y, z)) {
                    throw new IndexOutOfBoundsException("Position " + x + ", " + y + ", " + z + " is out of bounds.");
                }
                return block.block(x, y, z);
            }

            @Override
            public Biome getBiome(int x, int y, int z) {
                if (!area.contains(x, y, z)) {
                    throw new IndexOutOfBoundsException("Position " + x + ", " + y + ", " + z + " is out of bounds.");
                }
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
        return from((x, y, z) -> block, (x, y, z) -> null, Area.block(pos));
    }

    static WorldView biome(Biome biome, Point pos) {
        return from((x, y, z) -> null, (x, y, z) -> biome, Area.fill(pos, pos.add(Instance.BIOME_SIZE)));
    }

    static Mutable view(Mutable worldView, Area area) {
        return new MutableViewWorldView(worldView, area);
    }

    static WorldView view(WorldView worldView, Area area) {
        return from(worldView::getBlock, worldView::getBiome, area);
    }

    static WorldView translate(WorldView worldView, Point translation) {
        return new TranslateWorldView(worldView, translation);
    }

    static Mutable translate(Mutable worldView, Point translation) {
        return new MutableTranslateWorldView(worldView, translation);
    }

    static IndexOutOfBoundsException outOfBounds() {
        return new IndexOutOfBoundsException("Out of bounds");
    }
}
