package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Arrays;
import java.util.function.Consumer;

import static net.minestom.server.instance.Instance.BIOME_SIZE;
import static net.minestom.server.instance.Instance.SECTION_SIZE;

class ChunkWorldView implements WorldView.Mutable {

    private final Block[] blocks; // xzy
    private final Biome[] biomes; // xzy
    private final Area area;

    public ChunkWorldView(int maxY, Block defaultBlock, Biome defaultBiome) {
        int sectionCount = maxY / SECTION_SIZE;
        this.blocks = new Block[SECTION_SIZE * SECTION_SIZE * (sectionCount * SECTION_SIZE)];
        this.biomes = new Biome[blocks.length / (BIOME_SIZE * BIOME_SIZE * BIOME_SIZE)];
        this.area = Area.fill(Vec.ZERO, new Vec(SECTION_SIZE).withY(maxY));
        Arrays.fill(blocks, defaultBlock);
        Arrays.fill(biomes, defaultBiome);
    }

    private int blockIndex(int x, int y, int z) {
        return x + (z * SECTION_SIZE) + (y * SECTION_SIZE * SECTION_SIZE);
    }

    private int biomeIndex(int x, int y, int z) {
        return x + (z * BIOME_SIZE) + (y * BIOME_SIZE * BIOME_SIZE);
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        if (!area.contains(x, y, z)) throw WorldView.outOfBounds();
        int index = blockIndex(x, y, z);
        blocks[index] = block;
    }

    @Override
    public @Nullable Block getBlock(int x, int y, int z, Condition condition) {
        if (!area.contains(x, y, z)) throw WorldView.outOfBounds();
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
        area.forEach((vec) -> {
            int index = blockIndex(vec.blockX(), vec.blockY(), vec.blockZ());
            blocks[index] = null;
        });
    }

    @Override
    public void setBiome(int x, int y, int z, Biome biome) {
        if (!area.contains(x, y, z)) throw WorldView.outOfBounds();
        int index = biomeIndex(x / BIOME_SIZE, y / BIOME_SIZE, z / BIOME_SIZE);
        biomes[index] = biome;
    }

    @Override
    public @Nullable Biome getBiome(int x, int y, int z) {
        if (!area.contains(x, y, z)) throw WorldView.outOfBounds();
        int index = biomeIndex(x / BIOME_SIZE, y / BIOME_SIZE, z / BIOME_SIZE);
        return biomes[index];
    }
}
