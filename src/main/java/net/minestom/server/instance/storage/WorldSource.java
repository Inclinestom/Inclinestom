package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.AnvilLoader;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.utils.async.AsyncUtils;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Interface implemented to change the way blocks are loaded/saved.
 * <p>
 * See {@link AnvilLoader} for the default implementation used in {@link InstanceContainer}.
 */
public interface WorldSource {

    /**
     * Attempts to load a {@link WorldView} that fully contains the given {@link Area}.
     *
     * @param area the view to load
     * @return a {@link CompletableFuture} completed once the {@link Area} has been loaded, and failed if the load
     * failed for any reason.
     */
    CompletableFuture<@Nullable WorldView> load(Area area);

    /**
     * Saves a {@link WorldView}.
     *
     * @param storage the storage to save
     * @return a {@link CompletableFuture} executed when the {@link WorldView} is done saving,
     * failed if the save failed for any reason. (not saving at all counts as a failure)
     */
    CompletableFuture<Void> save(WorldView storage);

    /**
     * Called when a view is unloaded, so that this world source can unload any resource it is holding.
     * Note: Minestom currently has no way to determine whether the chunk comes from this loader, so you may get
     * unload requests for chunks not created by the loader.
     *
     * @param area the view to unload
     * @return a {@link CompletableFuture} executed when the {@link Area}'s cache is done unloading.
     */
    default CompletableFuture<Void> unloadArea(Area area) {
        return AsyncUtils.VOID_FUTURE;
    }

    static WorldSource empty() {
        return new WorldSource() {
            @Override
            public CompletableFuture<@Nullable WorldView> load(Area area) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> save(WorldView storage) {
                return CompletableFuture.failedFuture(new UnsupportedOperationException("Cannot save to an empty world source"));
            }
        };
    }
}
