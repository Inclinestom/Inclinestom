package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.instance.storage.WorldView;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.world.biomes.Biome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static net.minestom.server.instance.GeneratorImpl.unit;
import static net.minestom.server.instance.Instance.SECTION_SIZE;
import static net.minestom.server.utils.chunk.ChunkUtils.ceilSection;
import static net.minestom.server.utils.chunk.ChunkUtils.floorSection;
import static org.junit.jupiter.api.Assertions.*;

public class GeneratorTest {

    @ParameterizedTest
    @MethodSource("sectionFloorParam")
    public void sectionFloor(int expected, int input) {
        assertEquals(expected, floorSection(input), "floorSection(" + input + ")");
    }

    private static Stream<Arguments> sectionFloorParam() {
        return Stream.of(Arguments.of(-32, -32),
                Arguments.of(-32, -31),
                Arguments.of(-32, -17),
                Arguments.of(-16, -16),
                Arguments.of(-16, -15),
                Arguments.of(0, 0),
                Arguments.of(0, 1),
                Arguments.of(0, 2),
                Arguments.of(16, 16),
                Arguments.of(16, 17));
    }

    @ParameterizedTest
    @MethodSource("sectionCeilParam")
    public void sectionCeil(int expected, int input) {
        assertEquals(expected, ceilSection(input), "ceilSection(" + input + ")");
    }

    private static Stream<Arguments> sectionCeilParam() {
        return Stream.of(Arguments.of(-32, -32),
                Arguments.of(-16, -31),
                Arguments.of(-16, -17),
                Arguments.of(-16, -16),
                Arguments.of(-0, -15),
                Arguments.of(0, 0),
                Arguments.of(16, 1),
                Arguments.of(16, 2),
                Arguments.of(16, 16),
                Arguments.of(32, 17));
    }

    @Test
    public void chunkSize() {
        final int minSection = 0;
        final int maxSection = 5;
        final int chunkX = 3;
        final int chunkZ = -2;
        final int sectionCount = maxSection - minSection;
        Point min = new Vec(chunkX * 16, minSection * 16, chunkZ * 16);
        Point max = new Vec(chunkX * 16 + 16, maxSection * 16, chunkZ * 16 + 16);
        GenerationUnit chunk = GeneratorImpl.mutable(Area.fill(min, max));
        assertEquals(new Vec(16, sectionCount * 16, 16), chunk.size());
        assertEquals(min, chunk.absoluteStart());
        assertEquals(max, chunk.absoluteEnd());
    }

    @Test
    public void chunkSizeNeg() {
        final int minSection = -1;
        final int maxSection = 5;
        final int chunkX = 3;
        final int chunkZ = -2;
        final int sectionCount = maxSection - minSection;
        Point min = new Vec(chunkX * 16, minSection * 16, chunkZ * 16);
        Point max = new Vec(chunkX * 16 + 16, maxSection * 16, chunkZ * 16 + 16);
        GenerationUnit chunk = GeneratorImpl.mutable(Area.fill(min, max));
        assertEquals(new Vec(16, sectionCount * 16, 16), chunk.size());
        assertEquals(min, chunk.absoluteStart());
        assertEquals(max, chunk.absoluteEnd());
    }

    @Test
    public void sectionSize() {
        final int sectionX = 3;
        final int sectionY = -5;
        final int sectionZ = -2;
        GenerationUnit section = GeneratorImpl.section(sectionX, sectionY, sectionZ, false);
        assertEquals(new Vec(16), section.size());
        assertEquals(new Vec(sectionX * 16, sectionY * 16, sectionZ * 16), section.absoluteStart());
        assertEquals(new Vec(sectionX * 16 + 16, sectionY * 16 + 16, sectionZ * 16 + 16), section.absoluteEnd());
    }

    @Test
    public void chunkAbsolute() {
        final int minSection = 0;
        final int maxSection = 5;
        final int chunkX = 3;
        final int chunkZ = -2;
        final int sectionCount = maxSection - minSection;
        Point min = new Vec(chunkX * 16, minSection * 16, chunkZ * 16);
        Area chunkArea = Area.fill(min, min.add(16).withY(maxSection * 16));
        WorldView.Mutable worldView = WorldView.mutable(chunkArea, WorldView.empty());
        var chunkUnits = GeneratorImpl.mutable(worldView);
        Generator generator = chunk -> {
            var modifier = chunk.modifier();
            assertThrows(Exception.class, () -> modifier.setBlock(0, 0, 0, Block.STONE), "Block outside of chunk");
            modifier.setBlock(56, 0, -25, Block.STONE);
            modifier.setBlock(56, 17, -25, Block.STONE);
        };
        generator.generate(chunkUnits);
        assertEquals(Block.STONE, worldView.getBlock(56, 0, -25));
        assertEquals(Block.STONE, worldView.getBlock(56, 17, -25));
    }

    @Test
    public void chunkAbsoluteAll() {
        final int minSection = 0;
        final int maxSection = 5;
        final int chunkX = 3;
        final int chunkZ = -2;
        final int sectionCount = maxSection - minSection;
        Point min = new Vec(chunkX * 16, minSection * 16, chunkZ * 16);
        Area chunkArea = Area.fill(min, min.add(16).withY(maxSection * 16));
        WorldView.Mutable worldView = WorldView.mutable(chunkArea, WorldView.empty());
        var chunkUnits = GeneratorImpl.mutable(worldView);
        Generator generator = chunk -> {
            var modifier = chunk.modifier();
            Set<Point> points = new HashSet<>();
            modifier.setAll((x, y, z) -> {
                assertTrue(points.add(new Vec(x, y, z)), "Duplicate point: " + x + ", " + y + ", " + z);
                assertEquals(chunkX, ChunkUtils.getSectionCoordinate(x));
                assertEquals(chunkZ, ChunkUtils.getSectionCoordinate(z));
                return Block.STONE;
            });
            assertEquals(16 * 16 * 16 * sectionCount, points.size());
        };
        generator.generate(chunkUnits);
        for (Point point : worldView.area()) {
            assertEquals(Block.STONE, worldView.getBlock(point));
        }
    }

    @Test
    public void chunkRelative() {
        final int minSection = -1;
        final int maxSection = 5;
        final int chunkX = 3;
        final int chunkZ = -2;
        final int sectionCount = maxSection - minSection;
        Point min = new Vec(chunkX * 16, minSection * 16, chunkZ * 16);
        Area chunkArea = Area.fill(min, min.add(16).withY(maxSection * 16));
        WorldView.Mutable worldView = WorldView.mutable(chunkArea, WorldView.empty());
        var chunkUnits = GeneratorImpl.mutable(worldView);
        Generator generator = chunk -> {
            var modifier = chunk.modifier();
            assertThrows(Exception.class, () -> modifier.setRelative(-1, 0, 0, Block.STONE));
            assertThrows(Exception.class, () -> modifier.setRelative(16, 0, 0, Block.STONE));
            assertThrows(Exception.class, () -> modifier.setRelative(17, 0, 0, Block.STONE));
            assertThrows(Exception.class, () -> modifier.setRelative(0, -1, 0, Block.STONE));
            assertThrows(Exception.class, () -> modifier.setRelative(0, 96, 0, Block.STONE));
            modifier.setRelative(0, 0, 0, Block.STONE);
            modifier.setRelative(0, 16, 2, Block.STONE);
            modifier.setRelative(5, 33, 5, Block.STONE);
        };
        generator.generate(chunkUnits);
        assertEquals(Block.STONE, worldView.getBlock(48, -16, -32));
        assertEquals(Block.STONE, worldView.getBlock(48, 0, -30));
        assertEquals(Block.STONE, worldView.getBlock(53, 17, -27));
    }

    @Test
    public void chunkRelativeAll() {
        final int minSection = -1;
        final int maxSection = 5;
        final int chunkX = 3;
        final int chunkZ = -2;
        final int sectionCount = maxSection - minSection;
        Point min = new Vec(chunkX * 16, minSection * 16, chunkZ * 16);
        Area chunkArea = Area.fill(min, min.add(16).withY(maxSection * 16));
        WorldView.Mutable worldView = WorldView.mutable(chunkArea, WorldView.empty());
        var chunkUnits = GeneratorImpl.mutable(worldView);
        Generator generator = chunk -> {
            var modifier = chunk.modifier();
            Set<Point> points = new HashSet<>();
            modifier.setAllRelative((x, y, z) -> {
                assertTrue(MathUtils.isBetween(x, 0, 16), "x out of bounds: " + x);
                assertTrue(MathUtils.isBetween(y, 0, sectionCount * 16), "y out of bounds: " + y);
                assertTrue(MathUtils.isBetween(z, 0, 16), "z out of bounds: " + z);
                assertTrue(points.add(new Vec(x, y, z)), "Duplicate point: " + x + ", " + y + ", " + z);
                return Block.STONE;
            });
            assertEquals(16 * 16 * 16 * sectionCount, points.size());
        };
        generator.generate(chunkUnits);
        for (Point point : worldView.area()) {
            assertEquals(Block.STONE, worldView.getBlock(point));
        }
    }

    @Test
    public void chunkBiomeSet() {
        final int minSection = -1;
        final int maxSection = 5;
        final int chunkX = 3;
        final int chunkZ = -2;
        Point min = new Vec(chunkX * 16, minSection * 16, chunkZ * 16);
        Area chunkArea = Area.fill(min, min.add(16).withY(maxSection * 16));
        WorldView.Mutable worldView = WorldView.mutable(chunkArea, WorldView.empty());
        var chunkUnits = GeneratorImpl.mutable(worldView);
        Generator generator = chunk -> {
            var modifier = chunk.modifier();
            modifier.setBiome(48, 0, -32, Biome.PLAINS);
            modifier.setBiome(48 + 8, 0, -32, Biome.PLAINS);
        };
        generator.generate(chunkUnits);
        assertEquals(Biome.PLAINS, worldView.getBiome(48, 0, -32));
        assertEquals(Biome.PLAINS, worldView.getBiome(48 + 8, 0, -32));
    }

    @Test
    public void chunkBiomeFill() {
        final int minSection = -1;
        final int maxSection = 5;
        final int chunkX = 3;
        final int chunkZ = -2;
        final int sectionCount = maxSection - minSection;
        WorldView.Mutable worldView = WorldView.mutable(Area.chunk(minSection * SECTION_SIZE, maxSection * SECTION_SIZE, chunkX, chunkZ));
        var chunkUnits = GeneratorImpl.mutable(worldView);
        Generator generator = chunk -> {
            var modifier = chunk.modifier();
            modifier.fillBiome(Biome.PLAINS);
        };
        generator.generate(chunkUnits);
        for (Point point : worldView.area()) {
            assertEquals(Biome.PLAINS, worldView.getBiome(point));
        }
    }

    @Test
    public void chunkFillHeightExact() {
        final int minSection = -1;
        final int maxSection = 5;
        final int sectionCount = maxSection - minSection;
        WorldView.Mutable worldView = WorldView.mutable(Area.chunk(minSection * SECTION_SIZE, maxSection * SECTION_SIZE, 0, 0));
        var chunkUnits = GeneratorImpl.mutable(worldView);
        Generator generator = chunk -> chunk.modifier().fillHeight(0, 32, Block.STONE);
        generator.generate(chunkUnits);

        for (Point point : worldView.area()) {
            Block block = worldView.getBlock(point);
            if (point.y() >= 0 && point.y() < 32) {
                assertEquals(Block.STONE, block);
            } else {
                assertEquals(Block.AIR, block);
            }
        }
    }

    @Test
    public void chunkFillHeightOneOff() {
        final int minSection = -1;
        final int maxSection = 5;
        final int sectionCount = maxSection - minSection;
        Point min = new Vec(0, minSection * 16, 0);
        Area chunkArea = Area.fill(min, min.add(16).withY(maxSection * 16));
        WorldView.Mutable worldView = WorldView.mutable(chunkArea, WorldView.empty());
        var chunkUnits = GeneratorImpl.mutable(worldView);
        Generator generator = chunk -> chunk.modifier().fillHeight(1, 33, Block.STONE);
        generator.generate(chunkUnits);

        for (Point point : worldView.area()) {
            Block block = worldView.getBlock(point);
            if (point.y() > 0 && point.y() < 33) {
                assertEquals(Block.STONE, block);
            } else {
                assertEquals(Block.AIR, block);
            }
        }
    }

    @Test
    public void sectionFill() {
        Point pos = new Vec(-1, -1, 0).mul(SECTION_SIZE);
        WorldView.Mutable section = WorldView.mutable(Area.section(pos));
        var chunkUnit = GeneratorImpl.section(section, -1, -1, 0, false);
        Generator generator = chunk -> chunk.modifier().fill(Block.STONE);
        generator.generate(chunkUnit);
        for (Point point : section.area()) {
            assertEquals(Block.STONE, section.getBlock(point));
        }
    }

    static GenerationUnit dummyUnit(Point start, Point end) {
        return unit(null, start, end, null);
    }
}
