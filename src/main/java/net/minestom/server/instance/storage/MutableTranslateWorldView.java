package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

record MutableTranslateWorldView(Mutable worldView, Area translatedArea, Point translation) implements WorldView.Mutable {

    public MutableTranslateWorldView(Mutable worldView, Point translation) {
        this(worldView, worldView.area().translate(translation), translation);
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        worldView.setBlock(x - translation.blockX(), y - translation.blockY(), z - translation.blockZ(), block);
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
        return worldView.getBlock(x - translation.blockX(), y - translation.blockY(), z - translation.blockZ(), condition);
    }

    @Override
    public @NotNull Area area() {
        return translatedArea;
    }

    @Override
    public void mutate(Consumer<Mutator> mutator) {
        worldView.mutate(backingMutator -> {
            mutator.accept(new Mutator() {
                @Override
                public void setBlock(int x, int y, int z, Block block) {
                    backingMutator.setBlock(x - translation.blockX(), y - translation.blockY(), z - translation.blockZ(), block);
                }

                @Override
                public void setBiome(int x, int y, int z, Biome biome) {
                    backingMutator.setBiome(x - translation.blockX(), y - translation.blockY(), z - translation.blockZ(), biome);
                }
            });
        });
    }

    @Override
    public void clear(Area area) {
        worldView.clear(area.translate(translation));
    }

    @Override
    public void setBiome(int x, int y, int z, Biome biome) {
        worldView.setBiome(x - translation.blockX(), y - translation.blockY(), z - translation.blockZ(), biome);
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        return worldView.getBiome(x - translation.blockX(), y - translation.blockY(), z - translation.blockZ());
    }
}
