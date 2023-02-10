package net.minestom.server.instance;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The {@link SharedInstance} is an instance that shares the same chunks as its linked {@link InstanceContainer},
 * entities are separated.
 */
public class SharedInstance extends Instance {
    private final InstanceContainer instanceContainer;

    public SharedInstance(UUID uniqueId, InstanceContainer instanceContainer) {
        super(uniqueId, instanceContainer.getDimensionType());
        this.instanceContainer = instanceContainer;
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        this.instanceContainer.setBlock(x, y, z, block);
    }

    @Override
    public boolean placeBlock(BlockHandler.Placement placement) {
        return instanceContainer.placeBlock(placement);
    }

    @Override
    public boolean breakBlock(Player player, Point blockPosition, BlockFace blockFace) {
        return instanceContainer.breakBlock(player, blockPosition, blockFace);
    }

    @Override
    public CompletableFuture<Chunk> loadChunk(int chunkX, int chunkZ) {
        return instanceContainer.loadChunk(chunkX, chunkZ);
    }

    @Override
    public CompletableFuture<Chunk> loadOptionalChunk(int chunkX, int chunkZ) {
        return instanceContainer.loadOptionalChunk(chunkX, chunkZ);
    }

    @Override
    public void unloadChunk(Chunk chunk) {
        instanceContainer.unloadChunk(chunk);
    }

    @Override
    public Chunk getChunk(int chunkX, int chunkZ) {
        return instanceContainer.getChunk(chunkX, chunkZ);
    }

    @Override
    public CompletableFuture<Void> saveInstance() {
        return instanceContainer.saveInstance();
    }

    @Override
    public CompletableFuture<Void> saveChunkToStorage(Chunk chunk) {
        return instanceContainer.saveChunkToStorage(chunk);
    }

    @Override
    public CompletableFuture<Void> saveChunksToStorage() {
        return instanceContainer.saveChunksToStorage();
    }

    @Override
    public @Nullable Generator generator() {
        return instanceContainer.generator();
    }

    @Override
    public void setGenerator(@Nullable Generator generator) {
        instanceContainer.setGenerator(generator);
    }

    @NotNull
    @Override
    public Collection<Chunk> getChunks() {
        return instanceContainer.loadedSections();
    }

    @Override
    public void enableAutoChunkLoad(boolean enable) {
        instanceContainer.enableAutoChunkLoad(enable);
    }

    @Override
    public boolean hasEnabledAutoChunkLoad() {
        return instanceContainer.hasEnabledAutoChunkLoad();
    }

    @Override
    public boolean isInVoid(Point point) {
        return instanceContainer.isInVoid(point);
    }

    /**
     * Gets the {@link InstanceContainer} from where this instance takes its chunks from.
     *
     * @return the associated {@link InstanceContainer}
     */
    public InstanceContainer getInstanceContainer() {
        return instanceContainer;
    }
}
