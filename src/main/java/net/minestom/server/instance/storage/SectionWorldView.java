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

    private static final int SECTION_SIZE = Instance.SECTION_SIZE;
    private static final int BIOME_SIZE = Instance.BIOME_SIZE;
    private final Area area;

    private final Block[] blocks = new Block[SECTION_SIZE * SECTION_SIZE * SECTION_SIZE];
    private final Block defaultBlock;
    private final Biome[] biomes = new Biome[BIOME_SIZE * BIOME_SIZE * BIOME_SIZE];
    private final Biome defaultBiome;

    public SectionWorldView(Block defaultBlock, Biome defaultBiome) {
        this.defaultBlock = defaultBlock;
        this.defaultBiome = defaultBiome;

        Arrays.fill(blocks, defaultBlock);
        Arrays.fill(biomes, defaultBiome);

        this.area = Area.fill(Vec.ZERO, new Vec(SECTION_SIZE));
    }

    private int blockIndex(int x, int y, int z) {
        x = x % SECTION_SIZE; y = y % SECTION_SIZE; z = z % SECTION_SIZE;
        return x + y * SECTION_SIZE + z * SECTION_SIZE * SECTION_SIZE;
    }
    private int biomeIndex(int x, int y, int z) {
        x = x % BIOME_SIZE; y = y % BIOME_SIZE; z = z % BIOME_SIZE;
        return x + y * BIOME_SIZE + z * BIOME_SIZE * BIOME_SIZE;
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        if (x < 0 || y < 0 || z < 0 || x >= SECTION_SIZE || y >= SECTION_SIZE || z >= SECTION_SIZE)
            throw WorldView.outOfBounds();
        int index = blockIndex(x, y, z);
        blocks[index] = block;
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        if (x < 0 || y < 0 || z < 0 || x >= SECTION_SIZE || y >= SECTION_SIZE || z >= SECTION_SIZE)
            throw WorldView.outOfBounds();
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
        if (x < 0 || y < 0 || z < 0 || x >= SECTION_SIZE || y >= SECTION_SIZE || z >= SECTION_SIZE)
            throw WorldView.outOfBounds();
        int index = biomeIndex(x / BIOME_SIZE, y / BIOME_SIZE, z / BIOME_SIZE);
        biomes[index] = biome;
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= SECTION_SIZE || y >= SECTION_SIZE || z >= SECTION_SIZE)
            throw WorldView.outOfBounds();
        int index = biomeIndex(x / BIOME_SIZE, y / BIOME_SIZE, z / BIOME_SIZE);
        return biomes[index];
    }
}
