package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Arrays;
import java.util.function.Consumer;

class SectionWorldView implements WorldView.Mutable {

    private static final int SIZE = Instance.SECTION_SIZE;
    private final Area area;

    private final Block[] blocks = new Block[SIZE * SIZE * SIZE];
    private final Block defaultBlock;
    private final Biome[] biomes = new Biome[SIZE * SIZE * SIZE];
    private final Biome defaultBiome;

    public SectionWorldView(Block defaultBlock, Biome defaultBiome, Point section) {
        this.defaultBlock = defaultBlock;
        this.defaultBiome = defaultBiome;

        Arrays.fill(blocks, defaultBlock);
        Arrays.fill(biomes, defaultBiome);

        this.area = Area.fill(section, section.add(SIZE));
    }

    private int blockIndex(int x, int y, int z) {
        x = x % SIZE; y = y % SIZE; z = z % SIZE;
        return x + y * SIZE + z * SIZE * SIZE;
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        int index = blockIndex(x, y, z);
        blocks[index] = block;
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        int index = blockIndex(x, y, z);
        return blocks[index];
    }

    @Override
    public @NotNull Area area() {
        return area;
    }

    @Override
    public void mutate(Consumer<Mutator> mutator) {
        mutator.accept(new Mutator() {
            @Override
            public void setBlock(int x, int y, int z, Block block) {
                SectionWorldView.this.setBlock(x, y, z, block);
            }

            @Override
            public void setBiome(int x, int y, int z, Biome biome) {
                SectionWorldView.this.setBiome(x, y, z, biome);
            }
        });
    }

    @Override
    public void clear(Area area) {
        for (Point point : this.area.overlap(area)) {
            setBlock(point.blockX(), point.blockY(), point.blockZ(), defaultBlock);
            setBiome(point.blockX(), point.blockY(), point.blockZ(), defaultBiome);
        }
    }

    @Override
    public void setBiome(int x, int y, int z, Biome biome) {
        int index = blockIndex(x, y, z);
        biomes[index] = biome;
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        int index = blockIndex(x, y, z);
        return biomes[index];
    }
}
