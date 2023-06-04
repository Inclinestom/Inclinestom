package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.math.IntRange;
import net.minestom.server.world.biomes.Biome;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MutableWorldViewTest {
    @Test
    public void properties() {
        WorldView.Mutable mutableWorldView = WorldView.mutable();

        assertEquals(mutableWorldView.area(), Area.full(), () -> "MutableWorldView view " + mutableWorldView.area() + " is not " + Area.full());
    }

    @Test
    public void blocks() {
        WorldView.Mutable mutableWorldView = WorldView.mutable(WorldView.empty());

        Random random = new Random();
        Set<Vec> points = IntStream.generate(() -> 0)
                .limit(512)
                .boxed()
                .map(i -> new Vec(
                        random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE),
                        random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE),
                        random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE)
                ))
                .collect(Collectors.toSet());

        // Default block is air
        for (Vec point : points) {
            assertEquals(mutableWorldView.getBlock(point), Block.AIR, "Block at " + point + " is not " + Block.AIR);
        }

        // Set all to stone
        for (Vec point : points) {
            mutableWorldView.setBlock(point, Block.STONE);
        }

        // Assert stone
        for (Vec point : points) {
            assertEquals(mutableWorldView.getBlock(point), Block.STONE, "Block at " + point + " is not " + Block.STONE);
        }
    }

    @Test
    public void biomes() {
        WorldView.Mutable mutableWorldView = WorldView.mutable(WorldView.empty());

        Random random = new Random();
        Set<Vec> points = IntStream.generate(() -> 0)
                .limit(512)
                .boxed()
                .map(i -> new Vec(
                        random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE),
                        random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE),
                        random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE)
                ))
                .collect(Collectors.toSet());

        // Default biome is plains
        for (Vec point : points) {
            assertEquals(mutableWorldView.getBiome(point), Biome.PLAINS, "Biome at " + point + " is not " + Biome.PLAINS);
        }

        // Set all to custom biome
        Biome customBiome = Biome.builder().name(NamespaceID.from("custom")).build();
        for (Vec point : points) {
            mutableWorldView.setBiome(point, customBiome);
        }

        // Assert desert
        for (Vec point : points) {
            assertEquals(mutableWorldView.getBiome(point), customBiome, "Biome at " + point + " is not " + customBiome);
        }
    }
}
