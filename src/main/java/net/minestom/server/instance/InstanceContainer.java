package net.minestom.server.instance;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ExperienceOrb;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.instance.InstanceChunkLoadEvent;
import net.minestom.server.event.instance.InstanceChunkUnloadEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.instance.palette.Palette;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.BlockEntityDataPacket;
import net.minestom.server.network.packet.server.play.EffectPacket;
import net.minestom.server.network.packet.server.play.UnloadChunkPacket;
import net.minestom.server.snapshot.InstanceSnapshot;
import net.minestom.server.snapshot.SnapshotUpdater;
import net.minestom.server.utils.PacketUtils;
import net.minestom.server.utils.async.AsyncUtils;
import net.minestom.server.utils.block.BlockUtils;
import net.minestom.server.utils.chunk.ChunkCache;
import net.minestom.server.utils.chunk.ChunkSupplier;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.utils.validate.Check;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import static net.minestom.server.utils.chunk.ChunkUtils.*;

/**
 * InstanceContainer is an instance that contains chunks in contrary to SharedInstance.
 */
public class InstanceContainer extends InstanceBase {
    private static final AnvilLoader DEFAULT_LOADER = new AnvilLoader("world");

    // the generator used, can be null
    private volatile Generator generator;
    private final BlockStorage.Union blockStorage = BlockStorage.union();

    private SectionLoader sectionLoader = SectionLoader.filled(Block.AIR);

    @ApiStatus.Experimental
    public InstanceContainer(UUID uniqueId, DimensionType dimensionType, @Nullable IChunkLoader loader) {
        super(uniqueId, dimensionType);
    }

    public InstanceContainer(UUID uniqueId, DimensionType dimensionType) {
        this(uniqueId, dimensionType, null);
    }

    @Override
    public @Nullable Block getBlock(int x, int y, int z, Condition condition) {
        return blockStorage.getBlock(x, y, z, condition);
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        if (!isAreaLoaded(Area.collection(new Vec(x, y, z)))) return;
        UNSAFE_setBlock(x, y, z, block);
    }

    /**
     * Sets a block at the specified position.
     */
    private synchronized void UNSAFE_setBlock(int x, int y, int z, Block block) {
        // Refresh the last block change time
        final Vec blockPosition = new Vec(x, y, z);

        final Block previousBlock = blockStorage.getBlock(blockPosition);
        final BlockHandler previousHandler = previousBlock.handler();

        // Change id based on neighbors
        final BlockPlacementRule blockPlacementRule = MinecraftServer.getBlockManager().getBlockPlacementRule(block);
        if (blockPlacementRule != null) {
            block = blockPlacementRule.blockUpdate(this, blockPosition, block);
        }

        // Set the block
        // TODO: reduce allocations
        BlockStorage.Mutable newStorage = BlockStorage.inMemory();
        newStorage.setBlock(x, y, z, block);
        blockStorage.add(newStorage);

        // Refresh neighbors since a new block has been placed
//        executeNeighboursBlockPlacementRule(blockPosition);

        // Refresh player chunk block
        {
            Set<Player> viewers = viewers(Area.collection(new Vec(x, y, z)));

            PacketUtils.sendGroupedPacket(viewers, new BlockChangePacket(blockPosition, block.stateId()));
            var registry = block.registry();
            if (registry.isBlockEntity()) {
                final NBTCompound data = BlockUtils.extractClientNbt(block);
                PacketUtils.sendGroupedPacket(viewers, new BlockEntityDataPacket(blockPosition, registry.blockEntityId(), data));
            }
        }

        if (previousHandler != null) {
            // Previous destroy
            previousHandler.onDestroy(new BlockHandler.Destroy(previousBlock, this, blockPosition));
        }
        final BlockHandler handler = block.handler();
        if (handler != null) {
            // New placement
            handler.onPlace(new BlockHandler.Placement(block, this, blockPosition));
        }
    }

    @Override
    public boolean placeBlock(BlockHandler.Placement placement) {
        final Point blockPosition = placement.getBlockPosition();
        if (!isAreaLoaded(Area.collection(blockPosition))) return false;
        UNSAFE_setBlock(blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ(),
                placement.getBlock());
        return true;
    }

    @Override
    public boolean breakBlock(Player player, Point blockPosition, BlockFace blockFace) {
        Area area = Area.collection(blockPosition);
        if (!isAreaLoaded(area)) {
            throw new IllegalStateException("Cannot break block at " + blockPosition + " because the area is not loaded");
        }

        final Block block = getBlock(blockPosition);
        final int x = blockPosition.blockX();
        final int y = blockPosition.blockY();
        final int z = blockPosition.blockZ();
        if (block.isAir()) {
            // The player probably has a wrong version of this chunk section, send it
//            chunk.sendChunk(player);
            return false;
        }
        PlayerBlockBreakEvent blockBreakEvent = new PlayerBlockBreakEvent(player, block, Block.AIR, blockPosition, blockFace);
        EventDispatcher.call(blockBreakEvent);
        final boolean allowed = !blockBreakEvent.isCancelled();
        if (allowed) {
            // Break or change the broken block based on event result
            final Block resultBlock = blockBreakEvent.getResultBlock();
            UNSAFE_setBlock(x, y, z, resultBlock);
            // Send the block break effect packet
            PacketUtils.sendGroupedPacket(viewers(area),
                    new EffectPacket(2001 /*Block break + block break sound*/, blockPosition, block.stateId(), false),
                    // Prevent the block breaker to play the particles and sound two times
                    (viewer) -> !viewer.equals(player));
        }
        return allowed;
    }

    @Override
    public CompletableFuture<BlockStorage> loadArea(Area area) {
        Point min = area.min();
        Point max = area.max();

        int minSectionX = (min.blockX() / Chunk.CHUNK_SECTION_SIZE) * min.blockX() / Chunk.CHUNK_SECTION_SIZE;
        int minSectionY = (min.blockY() / Chunk.CHUNK_SECTION_SIZE) * min.blockY() / Chunk.CHUNK_SECTION_SIZE;
        int minSectionZ = (min.blockZ() / Chunk.CHUNK_SECTION_SIZE) * min.blockZ() / Chunk.CHUNK_SECTION_SIZE;

        List<CompletableFuture<@Nullable BlockStorage>> futures = new ArrayList<>();

        for (int x = minSectionX; x < max.blockX(); x += Chunk.CHUNK_SECTION_SIZE) {
            for (int y = minSectionY; y < max.blockY(); y += Chunk.CHUNK_SECTION_SIZE) {
                for (int z = minSectionZ; z < max.blockZ(); z += Chunk.CHUNK_SECTION_SIZE) {
                    Vec sectionPos = new Vec(x, y, z);
                    if (!loadedSections().contains(sectionPos)) {
                        futures.add(sectionLoader.load(sectionPos));
                    }
                }
            }
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        return allOf.thenApply(ignored -> {
            for (CompletableFuture<@Nullable BlockStorage> future : futures) {
                BlockStorage storage = future.join();
                if (storage != null) blockStorage.add(storage);
            }
            return BlockStorage.view(blockStorage, area);
        });
    }

    @Override
    public CompletableFuture<Void> unloadArea(Area area) {
        blockStorage.clear(area);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @Nullable BlockStorage storage(Area area) {
        return BlockStorage.view(blockStorage, area);
    }

    @Override
    public @Nullable BlockStorage storage() {
        return blockStorage;
    }

    @Override
    public boolean isAreaLoaded(Area area) {
        return blockStorage.area().contains(area);
    }

    @Override
    public CompletableFuture<Void> save() {
        return null;
    }

    @Override
    public @Nullable Generator generator() {
        return generator;
    }

    @Override
    public void setGenerator(@Nullable Generator generator) {
        this.generator = generator;
    }

    @Override
    public Set<Vec> loadedSections() {
        return null;
    }

    @Override
    public void setAreaLoadRule(AreaLoadRule areaLoadRule) {

    }

    @Override
    public DimensionType dimensionType() {
        return null;
    }

    @Override
    public WorldBorder worldBorder() {
        return null;
    }

    @Override
    public Set<Player> viewers(Area area) {
        return null;
    }

    @Override
    public Set<Entity> entities() {
        return null;
    }

    @Override
    public Set<Player> players() {
        return null;
    }

    @Override
    public Set<EntityCreature> creatures() {
        return null;
    }

    @Override
    public Set<ExperienceOrb> experienceOrbs() {
        return null;
    }

    @Override
    public Set<Entity> areaEntities(Area area) {
        return null;
    }

    @Override
    public Collection<Entity> nearbyEntities(Point point, double range) {
        return null;
    }

    @Override
    public EntityTracker entityTracker() {
        return null;
    }

    @Override
    public UUID uniqueId() {
        return null;
    }

    @Override
    public InstanceSnapshot updateSnapshot(SnapshotUpdater updater) {
        return null;
    }
}
