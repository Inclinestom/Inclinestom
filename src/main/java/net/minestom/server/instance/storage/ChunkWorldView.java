package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

import static net.minestom.server.instance.Instance.BIOME_SIZE;
import static net.minestom.server.instance.Instance.SECTION_SIZE;

class ChunkWorldView implements WorldView.Mutable {

    private final Block[] blocks; // xzy
    private final Biome[] biomes; // xzy
    private final int minSection;
    private final Area area;

    public ChunkWorldView(DimensionType dimensionType, Block defaultBlock, Biome defaultBiome, int chunkX, int chunkZ) {
        int minY = dimensionType.getMinY();
        int maxY = dimensionType.getMaxY();
        int sectionCount = (maxY - minY) / SECTION_SIZE;
        this.blocks = new Block[SECTION_SIZE * SECTION_SIZE * (sectionCount * SECTION_SIZE)];
        this.biomes = new Biome[blocks.length / (BIOME_SIZE * BIOME_SIZE * BIOME_SIZE)];
        this.minSection = minY / SECTION_SIZE;
        this.area = Area.chunk(dimensionType, chunkX, chunkZ);
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        int sectionY = y / SECTION_SIZE;
        int sectionIndex = sectionY - minSection;
        int index = x + (z * SECTION_SIZE) + (sectionIndex * SECTION_SIZE * SECTION_SIZE);
        blocks[index] = block;
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        int sectionY = y / SECTION_SIZE;
        int sectionIndex = sectionY - minSection;
        int index = x + (z * SECTION_SIZE) + (sectionIndex * SECTION_SIZE * SECTION_SIZE);
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
                ChunkWorldView.this.setBlock(x, y, z, block);
            }

            @Override
            public void setBiome(int x, int y, int z, Biome biome) {
                ChunkWorldView.this.setBiome(x, y, z, biome);
            }
        });
    }

    @Override
    public void clear(Area area) {

    }

    @Override
    public void setBiome(int x, int y, int z, Biome biome) {
        int sectionY = y / SECTION_SIZE;
        int sectionIndex = sectionY - minSection;
        int index = x + (z * SECTION_SIZE) + (sectionIndex * SECTION_SIZE * SECTION_SIZE);
        biomes[index / (BIOME_SIZE * BIOME_SIZE * BIOME_SIZE)] = biome;
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        int sectionY = y / SECTION_SIZE;
        int sectionIndex = sectionY - minSection;
        int index = x + (z * SECTION_SIZE) + (sectionIndex * SECTION_SIZE * SECTION_SIZE);
        return biomes[index / (BIOME_SIZE * BIOME_SIZE * BIOME_SIZE)];
    }
}
