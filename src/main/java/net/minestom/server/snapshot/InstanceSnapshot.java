package net.minestom.server.snapshot;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Collection;
import java.util.Objects;

import static net.minestom.server.utils.chunk.ChunkUtils.getChunkCoordinate;

public sealed interface InstanceSnapshot extends Snapshot, Block.Getter, Biome.Getter, TagReadable
        permits SnapshotImpl.Instance {
    DimensionType dimensionType();

    long worldAge();

    long time();

    @Override
    default @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        SectionStorageSnapshot chunk = chunk(getChunkCoordinate(x), getChunkCoordinate(z));
        return Objects.requireNonNull(chunk).getBlock(x, y, z, condition);
    }

    @Override
    default Biome getBiome(int x, int y, int z) {
        SectionStorageSnapshot chunk = chunk(getChunkCoordinate(x), getChunkCoordinate(z));
        return Objects.requireNonNull(chunk).getBiome(x, y, z);
    }

    @Nullable SectionStorageSnapshot chunk(int chunkX, int chunkZ);

    default @Nullable SectionStorageSnapshot chunkAt(Point point) {
        return chunk(point.chunkX(), point.chunkZ());
    }

    Collection<SectionStorageSnapshot> chunks();

    Collection<EntitySnapshot> entities();

    ServerSnapshot server();
}
