package net.minestom.server.utils;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.function.IntegerBiConsumer;

import java.util.function.Consumer;

public class AreaUtils {

    public static void forEachChunk(Area area, IntegerBiConsumer consumer) {
        Point min = area.min();
        Point max = area.max();

        int minWorldViewX = min.sectionX();
        int minWorldViewZ = min.sectionZ();
        int maxWorldViewX = max.sectionX();
        int maxWorldViewZ = max.sectionZ();

        for (int chunkX = minWorldViewX; chunkX <= maxWorldViewX; chunkX++) {
            for (int chunkZ = minWorldViewZ; chunkZ <= maxWorldViewZ; chunkZ++) {
                consumer.accept(chunkX, chunkZ);
            }
        }
    }

    public static void forEachSection(Area area, Consumer<Vec> positionConsumer) {
        Point min = area.min();
        Point max = area.max();

        forEachChunk(area, (chunkX, chunkZ) -> {
            for (int sectionY = min.sectionY(); sectionY <= max.sectionY(); sectionY++) {
                positionConsumer.accept(new Vec(chunkX, sectionY, chunkZ));
            }
        });
    }
}
