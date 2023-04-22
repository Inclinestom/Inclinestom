package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.storage.WorldView;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface WorldLoader {
    static WorldLoader filled(Block block, Biome biome) {
        WorldView filled = WorldView.filled(block, biome);
        return area -> CompletableFuture.completedFuture(WorldView.view(filled, area));
    }

    static WorldLoader empty() {
        return area -> CompletableFuture.completedFuture(null);
    }

    /**
     * Attempts to load the world data within the given {@link Area}.
     * @param area the worldView to load
     * @return the loaded world data, or null if the worldView could not be loaded
     */
    CompletableFuture<@Nullable WorldView> load(Area area);

    /**
     * Attempts to save the given {@link WorldView}.
     * <p>
     *     NOTE: This method may not actually save the data. Not all implementations must support saving.
     * </p>
     * @param storage the world data to save
     * @return a future which completes when the world data has been saved.
     */
    default CompletableFuture<Void> save(WorldView storage) {
        return CompletableFuture.completedFuture(null);
    }
}
