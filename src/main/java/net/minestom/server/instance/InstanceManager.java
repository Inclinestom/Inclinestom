package net.minestom.server.instance;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.storage.WorldSource;
import net.minestom.server.utils.AreaUtils;
import net.minestom.server.utils.validate.Check;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Used to register {@link Instance}.
 */
public final class InstanceManager {

    private final Set<Instance> instances = new CopyOnWriteArraySet<>();

    /**
     * Registers an {@link Instance} internally.
     * <p>
     * Note: not necessary if you created your instance using {@link #createInstanceContainer()}
     * but only if you instantiated your instance object manually
     *
     * @param instance the {@link Instance} to register
     */
    public void registerInstance(Instance instance) {
        UNSAFE_registerInstance(instance);
    }

    /**
     * Creates and register an {@link InstanceContainer} with the specified {@link DimensionType}.
     *
     * @param dimensionType the {@link DimensionType} of the instance
     * @param loader        the chunk loader
     * @return the created {@link InstanceContainer}
     */
    @ApiStatus.Experimental
    public InstanceContainer createInstanceContainer(DimensionType dimensionType, @Nullable WorldSource loader) {
        final InstanceContainer instanceContainer = new InstanceContainer(UUID.randomUUID(), dimensionType, loader);
        registerInstance(instanceContainer);
        return instanceContainer;
    }

    public InstanceContainer createInstanceContainer(DimensionType dimensionType) {
        return createInstanceContainer(dimensionType, null);
    }

    @ApiStatus.Experimental
    public InstanceContainer createInstanceContainer(@Nullable WorldSource loader) {
        return createInstanceContainer(DimensionType.OVERWORLD, loader);
    }

    /**
     * Creates and register an {@link InstanceContainer}.
     *
     * @return the created {@link InstanceContainer}
     */
    public InstanceContainer createInstanceContainer() {
        return createInstanceContainer(DimensionType.OVERWORLD, null);
    }

    /**
     * Unregisters the {@link Instance} internally.
     * <p>
     * If {@code instance} is an {@link InstanceContainer} all chunks are unloaded.
     *
     * @param instance the {@link Instance} to unregister
     */
    public void unregisterInstance(Instance instance) {
        Check.stateCondition(!instance.getPlayers().isEmpty(), "You cannot unregister an instance with players inside.");
        synchronized (instance) {
            // Unload all chunks
            if (instance instanceof InstanceContainer) {
                instance.unloadArea(Area.full()).join();
                var dispatcher = MinecraftServer.process().dispatcher();
                AreaUtils.forEachSection(instance.loadedArea(), pos ->
                        dispatcher.deletePartition(Area.section(pos)));
            }
            // Unregister
            this.instances.remove(instance);
        }
    }

    /**
     * Gets all the registered instances.
     *
     * @return an unmodifiable {@link Set} containing all the registered instances
     */
    public Set<Instance> getInstances() {
        return Collections.unmodifiableSet(instances);
    }

    /**
     * Gets an instance by the given UUID.
     *
     * @param uuid UUID of the instance
     * @return the instance with the given UUID, null if not found
     */
    public @Nullable Instance getInstance(UUID uuid) {
        Optional<Instance> instance = getInstances()
                .stream()
                .filter(someInstance -> someInstance.uniqueId().equals(uuid))
                .findFirst();
        return instance.orElse(null);
    }

    /**
     * Registers an {@link Instance} internally.
     *
     * @param instance the {@link Instance} to register
     */
    private void UNSAFE_registerInstance(Instance instance) {
        this.instances.add(instance);
        var dispatcher = MinecraftServer.process().dispatcher();
        AreaUtils.forEachSection(instance.loadedArea(), pos ->
                dispatcher.deletePartition(Area.section(pos)));
    }
}
