package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

class UnionBlockStorage implements BlockStorage.Union {

    private Area area = Area.empty();

    private final List<BlockStorage> storages = new CopyOnWriteArrayList<>();

    @Override
    public void add(BlockStorage storage) {
        storages.add(storage);
        updateAreas();
    }

    private void updateAreas() {
        Area[] areas = storages.stream().map(BlockStorage::area).toArray(Area[]::new);
        this.area = Area.union(areas);
    }

    @Override
    public boolean remove(BlockStorage storage) {
        if (storages.remove(storage)) {
            updateAreas();
            return true;
        }
        return false;
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        for (int i = storages.size() - 1; i >= 0; i--) {
            BlockStorage storage = storages.get(i);
            Area area = storage.area();

            if (!area.contains(x, y, z)) continue;

            Block block = storage.getBlock(x, y, z, condition);
            if (block != null) return block;
        }
        return null;
    }

    @Override
    public Area area() {
        return area;
    }

    @Override
    public void mutate(Consumer<Block.Setter> mutator) {
        BlockStorage.Mutable mutable = BlockStorage.inMemory();
        mutable.mutate(mutator);
        add(mutable);
    }

    @Override
    public void clear(Area area) {
        List<BlockStorage> newStorages = new ArrayList<>();

        for (BlockStorage storage : this.storages) {
            Area storageArea = storage.area();
            Area excluded = Area.exclude(storageArea, area);
            if (excluded.size() == 0) continue;

            BlockStorage newStorage = BlockStorage.view(storage, excluded);
            newStorages.add(newStorage);
        }

        this.storages.clear();
        this.storages.addAll(newStorages);
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        BlockStorage.Mutable mutable = BlockStorage.inMemory();
        mutable.setBlock(x, y, z, block);
        add(mutable);
    }
}
