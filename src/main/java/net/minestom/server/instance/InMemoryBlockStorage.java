package net.minestom.server.instance;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.UnknownNullability;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

class InMemoryBlockStorage implements BlockStorage.Mutable {

    private final Long2ObjectMap<Int2ObjectMap<Block>> storage = new Long2ObjectOpenHashMap<>();
    private Area area = Area.empty();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Int2ObjectMap<Block> getOrCreate(int x, int z) {
        long key = index(x, z);
        return storage.computeIfAbsent(key, k -> new Int2ObjectOpenHashMap<>());
    }

    private long index(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private void updateBounds(int x, int y, int z) {
        if (area.contains(x, y, z)) return;
        area = Area.union(area, Area.collection(new Vec(x, y, z)));
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        try {
            lock.writeLock().lock();
            getOrCreate(x, z).put(y, block);
            updateBounds(x, y, z);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        try {
            lock.readLock().lock();
            long key = index(x, z);
            Int2ObjectMap<Block> map = storage.get(key);
            if (map == null) return null;
            return map.get(y);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void mutate(Consumer<Block.Setter> mutator) {
        try {
            lock.writeLock().lock();
            mutator.accept((x, y, z, block) -> {
                getOrCreate(x, z).put(y, block);
                updateBounds(x, y, z);
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear(Area area) {
        try {
            lock.writeLock().lock();
            area.forEach(point -> {
                long key = index(point.blockX(), point.blockZ());
                Int2ObjectMap<Block> map = storage.get(key);
                if (map == null) return;
                map.remove(point.blockY());
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Area area() {
        return area;
    }
}
