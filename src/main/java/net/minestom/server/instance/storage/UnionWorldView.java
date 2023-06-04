package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

class UnionWorldView implements WorldView.Union {

    private Area area = Area.empty();
    private boolean optimizedArea = true;

    private final List<WorldView> storages = new CopyOnWriteArrayList<>();

    @Override
    public void add(WorldView storage) {
        storages.add(storage);
        updateAreas();
    }

    private void updateAreas() {
        Area[] areas = storages.stream().map(WorldView::area).toArray(Area[]::new);
        this.area = Area.union(areas);
        this.optimizedArea = false;
    }

    @Override
    public boolean remove(WorldView storage) {
        if (storages.remove(storage)) {
            updateAreas();
            return true;
        }
        return false;
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        if (!this.area().contains(x, y, z)) {
            throw WorldView.outOfBounds();
        }
        for (int i = storages.size() - 1; i >= 0; i--) {
            WorldView storage = storages.get(i);
            Area area = storage.area();

            if (!area.contains(x, y, z)) continue;

            Block block = storage.getBlock(x, y, z, condition);
            if (block != null) return block;
        }
        return null;
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        if (!this.area().contains(x, y, z)) {
            throw WorldView.outOfBounds();
        }
        for (int i = storages.size() - 1; i >= 0; i--) {
            WorldView storage = storages.get(i);
            Area area = storage.area();

            if (!area.contains(x, y, z)) continue;

            Biome biome = storage.getBiome(x, y, z);
            if (biome != null) return biome;
        }
        return null;
    }

    @Override
    public Area area() {
        if (!optimizedArea) {
            optimizedArea = true;
            area = Area.optimize(area);
        }
        return area;
    }

    @Override
    public void mutate(Consumer<Mutator> mutator) {
        WorldView.Mutable mutable = WorldView.mutable();
        mutator.accept(new Mutator() {
            @Override
            public void setBlock(int x, int y, int z, Block block) {
                mutable.setBlock(x, y, z, block);
            }

            @Override
            public void setBiome(int x, int y, int z, Biome biome) {
                mutable.setBiome(x, y, z, biome);
            }
        });
        add(mutable);
    }

    @Override
    public void clear(Area area) {
        List<WorldView> newStorages = new ArrayList<>();

        for (WorldView storage : this.storages) {
            Area storageArea = storage.area();
            Area excluded = Area.exclude(storageArea, area);
            if (excluded.size() == 0) continue;

            WorldView newStorage = WorldView.view(storage, excluded);
            newStorages.add(newStorage);
        }

        this.storages.clear();
        this.storages.addAll(newStorages);
        updateAreas();
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        add(WorldView.block(block, new Vec(x, y, z)));
    }

    @Override
    public void setBiome(int x, int y, int z, Biome biome) {
        add(WorldView.biome(biome, new Vec(x, y, z)));
    }
}
