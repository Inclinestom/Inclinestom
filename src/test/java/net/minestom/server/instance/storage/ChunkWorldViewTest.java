package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
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
    private static final int CHUNK_MIN_Y = OVERWORLD.getMinY();
    private static final int CHUNK_MAX_Y = OVERWORLD.getMaxY();
    private static final int CHUNK_WIDTH = Instance.SECTION_SIZE;

    @Test
    public void properties() {
        WorldView.Mutable chunk = WorldView.mutable(Area.chunk(OVERWORLD));

        Point expectedMin = new Vec(0, CHUNK_MIN_Y, 0);
        Point expectedMax = new Vec(CHUNK_WIDTH, CHUNK_MAX_Y, CHUNK_WIDTH);
        double expectedSize = CHUNK_WIDTH * CHUNK_WIDTH * (CHUNK_MAX_Y - CHUNK_MIN_Y);
        assertEquals(chunk.area().min(), expectedMin, () -> "Chunk view " + chunk.area() + " does not start at " + expectedMin);
        assertEquals(chunk.area().max(), expectedMax, () -> "Chunk view " + chunk.area() + " does not end at " + expectedMax);
        assertEquals(chunk.area().size(), expectedSize, () -> "Chunk view " + chunk.area() + " is not " + expectedSize);
    }

    @Test
    public void blocks() {
        WorldView.Mutable chunk = WorldView.mutable(Area.chunk(OVERWORLD));

        // Default block is air
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = CHUNK_MIN_Y; y < CHUNK_MAX_Y; y++) {
                for (int z = 0; z < CHUNK_WIDTH; z++) {
                    assertEquals(chunk.getBlock(x, y, z), Block.AIR, "Block at " + new Vec(x, y, z) + " is not " + Block.AIR);
                }
            }
        }

        // Set all to stone
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = CHUNK_MIN_Y; y < CHUNK_MAX_Y; y++) {
                for (int z = 0; z < CHUNK_WIDTH; z++) {
                    chunk.setBlock(x, y, z, Block.STONE);
                }
            }
        }

        // Assert stone
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = CHUNK_MIN_Y; y < CHUNK_MAX_Y; y++) {
                for (int z = 0; z < CHUNK_WIDTH; z++) {
                    assertEquals(chunk.getBlock(x, y, z), Block.STONE, "Block at " + new Vec(x, y, z) + " is not " + Block.STONE);
                }
            }
        }
    }

    @Test
    public void biomes() {
        WorldView.Mutable chunk = WorldView.mutable(Area.chunk(OVERWORLD));

        // Default biome is plains
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = CHUNK_MIN_Y; y < CHUNK_MAX_Y; y++) {
                for (int z = 0; z < CHUNK_WIDTH; z++) {
                    assertEquals(chunk.getBiome(x, y, z), Biome.PLAINS, "Biome at " + new Vec(x, y, z) + " is not " + Biome.PLAINS);
                }
            }
        }

        // Set all to custom biome
        Biome customBiome = Biome.builder().name(NamespaceID.from("custom")).build();
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = CHUNK_MIN_Y; y < CHUNK_MAX_Y; y++) {
                for (int z = 0; z < CHUNK_WIDTH; z++) {
                    chunk.setBiome(x, y, z, customBiome);
                }
            }
        }

        // Assert custom biome
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = CHUNK_MIN_Y; y < CHUNK_MAX_Y; y++) {
                for (int z = 0; z < CHUNK_WIDTH; z++) {
                    assertEquals(chunk.getBiome(x, y, z), customBiome, "Biome at " + new Vec(x, y, z) + " is not " + customBiome);
                }
            }
        }
    }

}
