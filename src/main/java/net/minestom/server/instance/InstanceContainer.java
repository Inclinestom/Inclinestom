package net.minestom.server.instance;

import net.minestom.server.MinecraftServer;
import net.minestom.server.Viewable;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ExperienceOrb;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.instance.storage.WorldSource;
import net.minestom.server.instance.storage.WorldView;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.server.utils.AreaUtils;
import net.minestom.server.utils.PacketUtils;
import net.minestom.server.utils.block.BlockUtils;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * InstanceContainer is an instance that contains chunks in contrary to SharedInstance.
 */
public class InstanceContainer extends InstanceBase {

    // the generator used, can be null
    private volatile Generator generator;
    private final WorldView.Union blockStorage = WorldView.union();
    private LoadingRule loadingRule = new PlayerRadiusLoadingRule(this);

    private WorldLoader worldLoader = WorldLoader.filled(Block.AIR, Biome.PLAINS);
    private final Map<Vec, List<WorldView>> forks = new ConcurrentHashMap<>();

    private final Map<Area, AreaViewable> viewable = new WeakHashMap<>();
    private WorldSource worldSource = new AnvilLoader(this, "world");

    @ApiStatus.Experimental
    public InstanceContainer(UUID uniqueId, DimensionType dimensionType, @Nullable WorldSource loader) {
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
        WorldView.Mutable newStorage = WorldView.inMemory();
        newStorage.setBlock(x, y, z, block);
        blockStorage.add(newStorage);

        // Refresh neighbors since a new block has been placed
//        executeNeighboursBlockPlacementRule(blockPosition);

        // Refresh player chunk block
        {
            Set<Player> viewers = viewers(Area.collection(new Vec(x, y, z))).getViewers();

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
            throw new IllegalStateException("Cannot break block at " + blockPosition + " because the worldView is not loaded");
        }

        final Block block = getBlock(blockPosition);
        final int x = blockPosition.blockX();
        final int y = blockPosition.blockY();
        final int z = blockPosition.blockZ();
        if (block.isAir()) {
            // The player probably has a wrong version of this chunk section, send it
//            chunk.sendWorldView(player);
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
            PacketUtils.sendGroupedPacket(viewers(area).getViewers(),
                    new EffectPacket(2001 /*Block break + block break sound*/, blockPosition, block.stateId(), false),
                    // Prevent the block breaker to play the particles and sound two times
                    (viewer) -> !viewer.equals(player));
        }
        return allowed;
    }

    private void addFork(Vec pos, WorldView view) {
        List<WorldView> views = forks.computeIfAbsent(pos, (p) -> new ArrayList<>());
        views.add(view);
    }

    private @Nullable List<WorldView> removeForks(Vec pos) {
        return forks.remove(pos);
    }

    private void applyFork(WorldView fork) {
        blockStorage.add(fork);
    }

    @Override
    public CompletableFuture<WorldView> loadArea(Area area) {
        Point min = area.min();
        Point max = area.max();

        return worldLoader.load(area).thenApplyAsync(storage -> {
            if (storage != null) {
                return storage;
            }

            // Generate the worldView
            if (generator == null) {
                throw new IllegalStateException("Cannot generate worldView " + area + " because no generator is set");
            }
            GeneratorImpl.UnitImpl unit = GeneratorImpl.mutable(area);
            generator.generate(unit);

            // Override old empty storage with filled one
            storage = ((GeneratorImpl.WorldViewModifierImpl) unit.modifier()).worldView();

            // Register forks or apply locally
            Area newTotalArea = Area.union(area, blockStorage.area());
            for (GeneratorImpl.UnitImpl forkUnit : unit.forks()) {
                GeneratorImpl.WorldViewModifierImpl forkAreaModifier = (GeneratorImpl.WorldViewModifierImpl) forkUnit.modifier();

                WorldView.Mutable fork = forkAreaModifier.worldView();

                if (newTotalArea.contains(fork.area())) {
                    // Apply now
                    applyFork(fork);
                } else {
                    // Register fork
                    AreaUtils.forEachSection(fork.area(), sectionPos -> {
                        Area section = Area.section(sectionPos);
                        addFork(sectionPos, WorldView.view(fork, section));
                    });
                }
            }

            // Apply external forks
            AreaUtils.forEachSection(area, sectionPos -> {
                List<WorldView> forks = removeForks(sectionPos);
                if (forks == null) return;
                for (WorldView fork : forks) {
                    applyFork(fork);
                }
            });

            return storage;
        }, ForkJoinPool.commonPool()).thenApply(storage -> {
            blockStorage.add(storage);
            return storage;
        });
    }

    @Override
    public CompletableFuture<Void> unloadArea(Area area) {
        blockStorage.clear(area);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @Nullable WorldView worldView(Area area) {
        return WorldView.view(blockStorage, area);
    }

    @Override
    public Area loadedArea() {
        return blockStorage.area();
    }

    @Override
    public boolean isAreaLoaded(Area area) {
        return blockStorage.area().contains(area);
    }

    @Override
    public CompletableFuture<Void> save() {
        return worldLoader.save(this.blockStorage);
    }

    @Override
    public @NotNull LoadingRule loadingRule() {
        return loadingRule;
    }

    @Override
    public void setLoadingRule(@NotNull LoadingRule loadingRule) {
        this.loadingRule = loadingRule;
    }

    @Override
    public @NotNull WorldSource worldSource() {
        return worldSource;
    }

    @Override
    public void setWorldSource(@NotNull WorldSource worldSource) {
        this.worldSource = worldSource;
    }

    @Override
    public void tick(long time) {
        // unloading/loading worldView
        Area newLoadedArea = loadingRule.update(blockStorage.area());
        Area toUnload = Area.exclude(blockStorage.area(), newLoadedArea);
        Area toLoad = Area.exclude(newLoadedArea, blockStorage.area());

        unloadArea(toUnload);
        loadArea(toLoad);

        super.tick(time);
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
    public void setTickingRule(LoadingRule loadingRule) {
        this.loadingRule = loadingRule;
    }

    @Override
    public Viewable viewers(Area area) {
        return viewable.computeIfAbsent(area, AreaViewable::new);
    }

    @Override
    public Set<Entity> entities() {
        return entityTracker().entities();
    }

    @Override
    public Set<Player> players() {
        return entityTracker().entities(EntityTracker.Target.PLAYERS);
    }

    @Override
    public Set<EntityCreature> creatures() {
        return entityTracker().entities()
                .stream()
                .filter(entity -> entity instanceof EntityCreature)
                .map(entity -> (EntityCreature) entity)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<ExperienceOrb> experienceOrbs() {
        return entityTracker().entities()
                .stream()
                .filter(entity -> entity instanceof ExperienceOrb)
                .map(entity -> (ExperienceOrb) entity)
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<Entity> areaEntities(Area area) {
        return entityTracker()
                .entities()
                .stream()
                .filter(entity -> area.contains(entity.getPosition()))
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<Entity> nearbyEntities(Point point, double range) {
        return entityTracker()
                .entities()
                .stream()
                .filter(entity -> entity.getPosition().distanceSquared(point) <= range * range)
                .collect(Collectors.toSet());
    }

    @Override
    public ChunkDataPacket chunkPacket(int chunkX, int chunkZ) {
        return ChunkUtils.chunkPacket(blockStorage, dimensionType(), chunkX, chunkZ);
    }

    private class AreaViewable implements Viewable {
        private final Area area;
        public AreaViewable(Area area) {
            this.area = area;
        }

        @Override
        public boolean addViewer(Player player) {
            if (player.getInstance() == InstanceContainer.this) return false;
            player.setInstance(InstanceContainer.this).join();
            return true;
        }

        @Override
        public boolean removeViewer(Player player) {
            if (player.getInstance() != InstanceContainer.this) return false;
            player.setInstance(null).join();
            return true;
        }

        @Override
        public Set<Player> getViewers() {
            return players().stream()
                    .filter(player -> area.contains(player.getPosition()))
                    .collect(Collectors.toSet());
        }
    }
}
