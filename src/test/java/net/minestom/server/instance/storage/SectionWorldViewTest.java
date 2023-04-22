package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.biomes.Biome;
import org.junit.jupiter.api.Test;

import static net.minestom.server.instance.Instance.BIOME_SIZE;
import static net.minestom.server.instance.Instance.SECTION_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SectionWorldViewTest {

    @Test
    public void properties() {
        SectionWorldView section = (SectionWorldView) WorldView.section();

        assertEquals(section.area().min(), Vec.ZERO, () -> "Section view " + section.area() + " does not start at " + Vec.ZERO);
        assertEquals(section.area().max(), new Vec(SECTION_SIZE), () -> "Section view " + section.area() + " does not end at " + new Vec(SECTION_SIZE));
        assertEquals(section.area().size(), SECTION_SIZE * SECTION_SIZE * SECTION_SIZE, () -> "Section view " + section.area() + " is not " + SECTION_SIZE * SECTION_SIZE * SECTION_SIZE);
    }

    @Test
    public void blocks() {
        SectionWorldView section = (SectionWorldView) WorldView.section();

        // Default block is air
        for (int x = 0; x < SECTION_SIZE; x++) {
            for (int y = 0; y < SECTION_SIZE; y++) {
                for (int z = 0; z < SECTION_SIZE; z++) {
                    assertEquals(section.getBlock(x, y, z), Block.AIR, "Block at " + new Vec(x, y, z) + " is not " + Block.AIR);
                }
            }
        }

        // Set all to stone
        for (int x = 0; x < SECTION_SIZE; x++) {
            for (int y = 0; y < SECTION_SIZE; y++) {
                for (int z = 0; z < SECTION_SIZE; z++) {
                    section.setBlock(x, y, z, Block.STONE);
                }
            }
        }

        // Assert stone
        for (int x = 0; x < SECTION_SIZE; x++) {
            for (int y = 0; y < SECTION_SIZE; y++) {
                for (int z = 0; z < SECTION_SIZE; z++) {
                    assertEquals(section.getBlock(x, y, z), Block.STONE, "Block at " + new Vec(x, y, z) + " is not " + Block.STONE);
                }
            }
        }
    }

    @Test
    public void biomes() {
        SectionWorldView section = (SectionWorldView) WorldView.section();

        for (int x = 0; x < SECTION_SIZE; x += BIOME_SIZE) {
            for (int y = 0; y < SECTION_SIZE; y += BIOME_SIZE) {
                for (int z = 0; z < SECTION_SIZE; z += BIOME_SIZE) {
                    assertEquals(section.getBiome(x, y, z), Biome.PLAINS, "Biome at " + new Vec(x, y, z) + " is not " + Biome.PLAINS);
                }
            }
        }

        // Set all to custom biome
        Biome customBiome = Biome.builder().name(NamespaceID.from("custom")).build();
        for (int x = 0; x < SECTION_SIZE; x += BIOME_SIZE) {
            for (int y = 0; y < SECTION_SIZE; y += BIOME_SIZE) {
                for (int z = 0; z < SECTION_SIZE; z += BIOME_SIZE) {
                    section.setBiome(x, y, z, customBiome);
                }
            }
        }

        // Assert custom biome
        for (int x = 0; x < SECTION_SIZE; x += BIOME_SIZE) {
            for (int y = 0; y < SECTION_SIZE; y += BIOME_SIZE) {
                for (int z = 0; z < SECTION_SIZE; z += BIOME_SIZE) {
                    assertEquals(section.getBiome(x, y, z), customBiome, "Biome at " + new Vec(x, y, z) + " is not " + customBiome);
                }
            }
        }
    }
}
