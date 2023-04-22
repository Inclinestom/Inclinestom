package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WorldViewTranslationTest {

    @Test
    public void translateFill() {
        WorldView filled = WorldView.filled(Block.STONE, Biome.PLAINS);
        WorldView view = WorldView.view(filled, Area.fill(new Vec(0, 0, 0), new Vec(10, 10, 10)));
        WorldView translated = WorldView.translate(view, new Vec(10, 10, 10));

        assertDoesNotThrow(() -> {
            for (int x = 10; x < 20; x++) {
                for (int y = 10; y < 20; y++) {
                    for (int z = 10; z < 20; z++) {
                        assertEquals(Block.STONE, translated.getBlock(x, y, z));
                        assertEquals(Biome.PLAINS, translated.getBiome(x, y, z));
                    }
                }
            }
        });

        assertThrows(NullPointerException.class, () -> {
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 10; y++) {
                    for (int z = 0; z < 10; z++) {
                        assertEquals(Block.STONE, translated.getBlock(x, y, z));
                        assertEquals(Biome.PLAINS, translated.getBiome(x, y, z));
                    }
                }
            }
        });
    }


    @Test
    public void translateMutable() {
        WorldView.Mutable mutable = WorldView.mutable(Area.fill(new Vec(-10, -10, -10), new Vec(10, 10, 10)));

        mutable.setBlock(-8, -8, -8, Block.STONE);
        mutable.setBlock(8, 8, 8, Block.STONE);

        WorldView.Mutable translated = WorldView.translate(mutable, new Vec(10, 10, 10));

        assertDoesNotThrow(() -> {
            assertEquals(Block.STONE, translated.getBlock(2, 2, 2));
            assertEquals(Block.STONE, translated.getBlock(18, 18, 18));
        });

        assertThrows(NullPointerException.class, () -> {
            assertEquals(Block.STONE, translated.getBlock(-2, -2, -2));
            assertEquals(Block.STONE, translated.getBlock(22, 22, 22));
        });
    }
}
