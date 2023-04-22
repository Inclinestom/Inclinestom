package net.minestom.server.utils;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.utils.function.IntegerBiConsumer;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class AreaUtils {

    public static void forEachChunk(Area area, IntegerBiConsumer consumer) {
        if (area.size() == 0) return;
        LongSet chunks = new LongOpenHashSet();
        for (Area fill : area.subdivide()) {
            Point min = fill.min();
            Point max = fill.max();

            int minChunkX = min.sectionX();
            int minChunkZ = min.sectionZ();

            for (int chunkX = minChunkX; chunkX <= max.sectionX(); chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= max.sectionZ(); chunkZ++) {
                    long index = ChunkUtils.getChunkIndex(chunkX, chunkZ);
                    if (chunks.contains(index)) continue;
                    chunks.add(index);
                    consumer.accept(chunkX, chunkZ);
                }
            }
        }
    }

    public static void forEachSection(Area area, Consumer<Vec> positionConsumer) {
        if (area.size() == 0) return;
        Set<Vec> sections = new HashSet<>();
        for (Area fill : area.subdivide()) {
            Point min = fill.min();
            Point max = fill.max();

            int minX = min.blockX();
            int minY = min.blockY();
            int minZ = min.blockZ();

            for (int x = minX; x <= max.blockX(); x++) {
                for (int y = minY; y <= max.blockY(); y++) {
                    for (int z = minZ; z <= max.blockZ(); z++) {
                        //noinspection IntegerDivisionInFloatingPointContext
                        Vec section = new Vec(x / Instance.SECTION_SIZE, y / Instance.SECTION_SIZE, z / Instance.SECTION_SIZE);
                        if (sections.contains(section)) continue;
                        sections.add(section);
                        positionConsumer.accept(section);
                    }
                }
            }
        }
    }
}
