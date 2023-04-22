package net.minestom.server.instance.storage;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

class MutableWorldView implements WorldView.Mutable {

    private final Long2ObjectMap<Int2ObjectMap<Block>> blocks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<Int2ObjectMap<Biome>> biomes = new Long2ObjectOpenHashMap<>();
    private Area area = Area.full();
    private final WorldView fallback;

    private final ReadWriteLock blockLock = new ReentrantReadWriteLock();
    private final ReadWriteLock biomeLock = new ReentrantReadWriteLock();
    public MutableWorldView(WorldView fallback) {
        this.fallback = fallback;
    }

    private @Nullable Block setBlockInternal(int x, int y, int z, Block block) {
        long key = index(x, z);
        Int2ObjectMap<Block> column = blocks.computeIfAbsent(key, k -> new Int2ObjectOpenHashMap<>());
        return column.put(y, block);
    }

    private @Nullable Block getBlockInternal(int x, int y, int z) {
        long key = index(x, z);
        Int2ObjectMap<Block> column = blocks.get(key);
        if (column == null) return fallback.getBlock(x, y, z);
        Block block = column.get(y);
        if (block == null) return fallback.getBlock(x, y, z);
        return block;
    }

    private @Nullable Biome setBiomeInternal(int x, int y, int z, Biome biome) {
        long key = index(x, z);
        Int2ObjectMap<Biome> column = biomes.computeIfAbsent(key, k -> new Int2ObjectOpenHashMap<>());
        return column.put(y, biome);
    }

    private @Nullable Biome getBiomeInternal(int x, int y, int z) {
        long key = index(x, z);
        Int2ObjectMap<Biome> column = biomes.get(key);
        if (column == null) return fallback.getBiome(x, y, z);
        Biome biome = column.get(y);
        if (biome == null) return fallback.getBiome(x, y, z);
        return biome;
    }



    private long index(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private void updateArea(int x, int y, int z) {
        if (area.contains(x, y, z)) return;
        area = Area.union(area, Area.collection(new Vec(x, y, z)));
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        try {
            blockLock.writeLock().lock();
            setBlockInternal(x, y, z, block);
            updateArea(x, y, z);
        } finally {
            blockLock.writeLock().unlock();
        }
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        try {
            blockLock.readLock().lock();
            return getBlockInternal(x, y, z);
        } finally {
            blockLock.readLock().unlock();
        }
    }

    @Override
    public void setBiome(int x, int y, int z, Biome biome) {
        try {
            biomeLock.writeLock().lock();
            setBiomeInternal(x, y, z, biome);
            updateArea(x, y, z);
        } finally {
            biomeLock.writeLock().unlock();
        }
    }

    @Override
    public @UnknownNullability Biome getBiome(int x, int y, int z) {
        try {
            biomeLock.readLock().lock();
            return getBiomeInternal(x, y, z);
        } finally {
            biomeLock.readLock().unlock();
        }
    }

    @Override
    public void mutate(Consumer<Mutator> consumer) {
        try {
            blockLock.writeLock().lock();
            biomeLock.writeLock().lock();
            consumer.accept(new Mutator() {
                @Override
                public void setBlock(int x, int y, int z, Block block) {
                    setBlockInternal(x, y, z, block);
                    updateArea(x, y, z);
                }

                @Override
                public void setBiome(int x, int y, int z, Biome biome) {
                    setBiomeInternal(x, y, z, biome);
                    updateArea(x, y, z);
                }
            });
        } finally {
            blockLock.writeLock().unlock();
            biomeLock.writeLock().unlock();
        }
    }

    @Override
    public void clear(Area area) {
        try {
            blockLock.writeLock().lock();
            area.forEach(point -> {
                long key = index(point.blockX(), point.blockZ());
                Int2ObjectMap<Block> map = blocks.get(key);
                if (map == null) return;
                map.remove(point.blockY());
            });
        } finally {
            blockLock.writeLock().unlock();
        }
    }

    @Override
    public Area area() {
        return area;
    }
}
