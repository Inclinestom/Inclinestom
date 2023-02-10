package net.minestom.server.instance;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.kyori.adventure.pointer.Pointers;
import net.minestom.server.MinecraftServer;
import net.minestom.server.Tickable;
import net.minestom.server.Viewable;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ExperienceOrb;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.pathfinding.PFInstanceSpace;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventHandler;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.network.packet.server.play.BlockActionPacket;
import net.minestom.server.network.packet.server.play.TimeUpdatePacket;
import net.minestom.server.snapshot.*;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.tag.Taggable;
import net.minestom.server.thread.ThreadDispatcher;
import net.minestom.server.timer.Schedulable;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.utils.ArrayUtils;
import net.minestom.server.utils.PacketUtils;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.utils.time.Cooldown;
import net.minestom.server.utils.validate.Check;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Instances are what are called "worlds" in Minecraft, you can add an entity in it using {@link Entity#setInstance(Instance)}.
 * <p>
 * An instance has entities and chunks, each instance contains its own entity list but the
 * chunk implementation has to be defined, see {@link InstanceContainer}.
 * <p>
 * WARNING: when making your own implementation registering the instance manually is required
 * with {@link InstanceManager#registerInstance(Instance)}, and
 * you need to be sure to signal the {@link ThreadDispatcher} of every partition/element changes.
 */
public interface Instance extends Block.Getter, Block.Setter,
        Tickable, Schedulable, Snapshotable, EventHandler<InstanceEvent>, Taggable, PacketGroupingAudience {

    @ApiStatus.Internal
    boolean placeBlock(BlockHandler.Placement placement);

    /**
     * Does call {@link net.minestom.server.event.player.PlayerBlockBreakEvent}
     * and send particle packets
     *
     * @param player        the {@link Player} who break the block
     * @param blockPosition the position of the broken block
     * @return true if the block has been broken, false if it has been cancelled
     */
    @ApiStatus.Internal
    boolean breakBlock(Player player, Point blockPosition, BlockFace blockFace);

    /**
     * Loads the {@link BlockStorage} necessary to entirely cover the given {@link Area}.
     * Implementations of this method typically load the chunks
     *
     * @param area Area
     * @return a {@link CompletableFuture} completed once the {@link Area} has been loaded, and failed if the load
     * failed for any reason.
     */
    CompletableFuture<BlockStorage> loadArea(Area area);

    /**
     * Unloads all the {@link BlockStorage} necessary to entirely cover the given {@link Area}.
     *
     * @param area the area to unload
     */
    CompletableFuture<Void> unloadArea(Area area);

    /**
     * Gets a {@link BlockStorage} which covers the given {@link Area}.
     *
     * @param area the area
     */
    @Nullable BlockStorage storage(Area area);

    /**
     * Gets a {@link BlockStorage} which covers all of the loaded area.
     */
    @Nullable BlockStorage storage();

    /**
     * Checks if the area is fully loaded.
     *
     * @param area the area
     * @return true if the area is loaded
     */
    boolean isAreaLoaded(Area area);

    /**
     * Indicate to the instance that you would like it to save.
     * There is NO GUARANTEE that the instance will actually save as instance implementations can be non-transient.
     *
     * @return the future called once the instance data has been saved
     */
    @ApiStatus.Experimental
    CompletableFuture<Void> save();

    /**
     * Gets the generator associated with the instance
     *
     * @return the generator if any
     */
    @Nullable Generator generator();

    /**
     * Changes the generator of the instance
     *
     * @param generator the new generator, or null to disable generation
     */
    void setGenerator(@Nullable Generator generator);

    /**
     * Gets all the instance's loaded sections.
     *
     * @return an unmodifiable containing all the instance's loaded sections
     */
    Set<Vec> loadedSections();

    /**
     * Sets the logic to use when deciding which areas of the world should be loaded.
     * @param areaLoadRule the new area load rule
     */
    void setAreaLoadRule(AreaLoadRule areaLoadRule);

    /**
     * Gets the instance {@link DimensionType}.
     *
     * @return the dimension of the instance
     */
    DimensionType dimensionType();

    /**
     * Gets the instance {@link WorldBorder};
     *
     * @return the {@link WorldBorder} linked to the instance
     */
    WorldBorder worldBorder();


    /**
     * Gets all the players which view any part of the given area.
     * @param area the area
     * @return the players viewing the area
     */
    Set<Player> viewers(Area area);

    /**
     * Gets the entities in the instance;
     *
     * @return an unmodifiable {@link Set} containing all the entities in the instance
     */
    Set<Entity> entities();

    /**
     * Gets the players in the instance;
     *
     * @return an unmodifiable {@link Set} containing all the players in the instance
     */
    Set<Player> players();

    /**
     * Gets the creatures in the instance;
     *
     * @return an unmodifiable {@link Set} containing all the creatures in the instance
     */
    @Deprecated
    Set<EntityCreature> creatures();

    /**
     * Gets the experience orbs in the instance.
     *
     * @return an unmodifiable {@link Set} containing all the experience orbs in the instance
     */
    @Deprecated
    Set<ExperienceOrb> experienceOrbs();

    /**
     * Gets the entities located in the area.
     *
     * @param area the area to get the entities from
     * @return an unmodifiable {@link Set} containing all the entities in a chunk,
     * if {@code chunk} is unloaded, return an empty {@link Set}
     */
    Set<Entity> areaEntities(Area area);

    /**
     * Gets nearby entities to the given position.
     *
     * @param point position to look at
     * @param range max range from the given point to collect entities at
     * @return entities that are not further than the specified distance from the transmitted position.
     */
    Collection<Entity> nearbyEntities(Point point, double range);

    @Override
    @Nullable Block getBlock(int x, int y, int z, Condition condition);

    /**
     * Sends a {@link BlockActionPacket} for all the viewers of the specific position.
     *
     * @param blockPosition the block position
     * @param actionId      the action id, depends on the block
     * @param actionParam   the action parameter, depends on the block
     * @see <a href="https://wiki.vg/Protocol#Block_Action">BlockActionPacket</a> for the action id &amp; param
     */
    void sendBlockAction(Point blockPosition, byte actionId, byte actionParam);

    @ApiStatus.Experimental
    EntityTracker entityTracker();

    /**
     * Gets the instance unique id.
     *
     * @return the instance unique id
     */
    UUID uniqueId();

    /**
     * Performs a single tick in the instance.
     * <p>
     * Warning: this does not update chunks and entities.
     *
     * @param time the tick time in milliseconds
     */
    @Override
    void tick(long time);

    @Override
    TagHandler tagHandler();

    @Override
    Scheduler scheduler();

    @Override
    @ApiStatus.Experimental
    EventNode<InstanceEvent> eventNode();

    @Override
    InstanceSnapshot updateSnapshot(SnapshotUpdater updater);

    @Override
    Pointers pointers();

    interface AreaLoadRule {
        void tick(Context context);

        interface Context {
            void load(Area area);
            void unload(Area area);
            Collection<Area> loaded();
        }
    }
}
