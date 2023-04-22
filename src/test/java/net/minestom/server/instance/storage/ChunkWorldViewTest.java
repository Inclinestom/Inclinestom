package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biomes.Biome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChunkWorldViewTest {

    private static final DimensionType OVERWORLD = DimensionType.OVERWORLD;
    private static final int CHUNK_MAX_Y = OVERWORLD.getMaxY() - OVERWORLD.getMinY();
    private static final int CHUNK_WIDTH = Instance.SECTION_SIZE;

    @Test
    public void properties() {
        ChunkWorldView chunk = (ChunkWorldView) WorldView.chunk(OVERWORLD);

        assertEquals(chunk.area().min(), Vec.ZERO, () -> "Chunk view " + chunk.area() + " does not start at " + Vec.ZERO);
        assertEquals(chunk.area().max(), new Vec(CHUNK_WIDTH).withY(CHUNK_MAX_Y), () -> "Chunk view " + chunk.area() + " does not end at " + new Vec(CHUNK_WIDTH).withY(CHUNK_MAX_Y));
        assertEquals(chunk.area().size(), (long) CHUNK_WIDTH * CHUNK_MAX_Y * CHUNK_WIDTH, () -> "Chunk view " + chunk.area() + " is not " + CHUNK_WIDTH * CHUNK_MAX_Y * CHUNK_WIDTH);
    }

    @Test
    public void blocks() {
        ChunkWorldView chunk = (ChunkWorldView) WorldView.chunk();

        // Default block is air
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_MAX_Y; y++) {
                for (int z = 0; z < CHUNK_WIDTH; z++) {
                    assertEquals(chunk.getBlock(x, y, z), Block.AIR, "Block at " + new Vec(x, y, z) + " is not " + Block.AIR);
                }
            }
        }

        // Set all to stone
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_MAX_Y; y++) {
                for (int z = 0; z < CHUNK_WIDTH; z++) {
                    chunk.setBlock(x, y, z, Block.STONE);
                }
            }
        }

        // Assert stone
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_MAX_Y; y++) {
                for (int z = 0; z < CHUNK_WIDTH; z++) {
                    assertEquals(chunk.getBlock(x, y, z), Block.STONE, "Block at " + new Vec(x, y, z) + " is not " + Block.STONE);
                }
            }
        }
    }

    @Test
    public void biomes() {
        ChunkWorldView chunk = (ChunkWorldView) WorldView.chunk();

        // Default biome is plains
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_MAX_Y; y++) {
                for (int z = 0; z < CHUNK_WIDTH; z++) {
                    assertEquals(chunk.getBiome(x, y, z), Biome.PLAINS, "Biome at " + new Vec(x, y, z) + " is not " + Biome.PLAINS);
                }
            }
        }

        // Set all to custom biome
        Biome customBiome = Biome.builder().name(NamespaceID.from("custom")).build();
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_MAX_Y; y++) {
                for (int z = 0; z < CHUNK_WIDTH; z++) {
                    chunk.setBiome(x, y, z, customBiome);
                }
            }
        }

        // Assert custom biome
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_MAX_Y; y++) {
                for (int z = 0; z < CHUNK_WIDTH; z++) {
                    assertEquals(chunk.getBiome(x, y, z), customBiome, "Biome at " + new Vec(x, y, z) + " is not " + customBiome);
                }
            }
        }
    }

}
