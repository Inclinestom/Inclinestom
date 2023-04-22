package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

record MutableViewWorldView(Mutable worldView, Area view) implements WorldView.Mutable {
    @Override
    public void setBlock(int x, int y, int z, Block block) {
        if (!view.contains(x, y, z)) throw WorldView.outOfBounds();
        worldView.setBlock(x, y, z, block);
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        if (!view.contains(x, y, z)) throw WorldView.outOfBounds();
        return worldView.getBlock(x, y, z, condition);
    }

    @Override
    public void mutate(Consumer<Mutator> mutator) {
        worldView.mutate(backingMutator -> {
            mutator.accept(new Mutator() {
                @Override
                public void setBlock(int x, int y, int z, Block block) {
                    if (!view.contains(x, y, z)) throw WorldView.outOfBounds();
                    backingMutator.setBlock(x, y, z, block);
                }

                @Override
                public void setBiome(int x, int y, int z, Biome biome) {
                    if (!view.contains(x, y, z)) throw WorldView.outOfBounds();
                    backingMutator.setBiome(x, y, z, biome);
                }
            });
        });
    }

    @Override
    public void clear(Area area) {
        Area newArea = area.overlap(this.view);
        worldView.clear(newArea);
    }

    @Override
    public void setBiome(int x, int y, int z, Biome biome) {
        if (!view.contains(x, y, z)) throw WorldView.outOfBounds();
        worldView.setBiome(x, y, z, biome);
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        if (!view.contains(x, y, z)) throw WorldView.outOfBounds();
        return worldView.getBiome(x, y, z);
    }

    @Override
    public @NotNull Area area() {
        return view;
    }
}
