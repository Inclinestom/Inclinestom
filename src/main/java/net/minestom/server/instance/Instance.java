package net.minestom.server.instance;

import com.extollit.gaming.ai.path.model.IInstanceSpace;
import net.minestom.server.Tickable;
import net.minestom.server.Viewable;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ExperienceOrb;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventHandler;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.instance.storage.WorldSource;
import net.minestom.server.instance.storage.WorldView;
import net.minestom.server.network.packet.server.play.BlockActionPacket;
import net.minestom.server.network.packet.server.play.ChunkDataPacket;
import net.minestom.server.network.packet.server.play.TimeUpdatePacket;
import net.minestom.server.snapshot.*;
import net.minestom.server.tag.Taggable;
import net.minestom.server.thread.ThreadDispatcher;
import net.minestom.server.timer.Schedulable;
import net.minestom.server.utils.PacketUtils;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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

    int SECTION_SIZE = 16;
    int BIOME_SIZE = 4;

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
     * Loads the {@link WorldView} necessary to entirely cover the given {@link Area}.
     * Implementations of this method typically load the chunks
     *
     * @param area Area
     * @return a {@link CompletableFuture} completed once the {@link Area} has been loaded, and failed if the load
     * failed for any reason.
     */
    CompletableFuture<WorldView> loadArea(Area area);

    /**
     * Unloads all the {@link WorldView} necessary to entirely cover the given {@link Area}.
     * <p>
     *     Implementations may choose to unload as much as they want, provided that they unload at least the given worldView.
     * </p>
     *
     * @param area the worldView to unload
     */
    CompletableFuture<Void> unloadArea(Area area);

    /**
     * Gets the {@link WorldView} which contains all loaded world data within the given {@link Area}.
     *
     * @param area the worldView
     * @return the world view
     */
    WorldView worldView(Area area);
    default WorldView worldView() {
        return worldView(loadedArea());
    }

    /**
     * Gets then {@link Area} which contains all loaded world data.
     *
     * @return the world view worldView
     */
    Area loadedArea();

    /**
     * Checks if the worldView is fully loaded.
     *
     * @param area the worldView
     * @return true if the worldView is loaded
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
     * Gets the loading rule used by the instance.
     *
     * @return the loading rule
     */
    @NotNull LoadingRule loadingRule();


    /**
     * Sets the loading rule used by the instance.
     *
     * @param loadingRule the new loading rule
     */
    void setLoadingRule(@NotNull LoadingRule loadingRule);

    /**
     * Gets the world source used by the instance.
     *
     * @return the world source
     */
    @NotNull WorldSource worldSource();

    /**
     * Sets the world source used by the instance.
     *
     * @param worldSource the new world source
     */
    void setWorldSource(@NotNull WorldSource worldSource);

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
     * Sets the logic to use when deciding which areas of the world should be loaded, and what the threading model should be.
     * By default, this is an instance of {@link PlayerRadiusLoadingRule}.
     * @param loadingRule the new worldView load rule
     */
    void setTickingRule(LoadingRule loadingRule);

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
     * Gets all the players which view any part of the given worldView.
     * @param area the worldView
     * @return the players viewing the worldView
     */
    Viewable viewers(Area area);

    /**
     * Gets all the players which view any part of the given chunk.
     * @param chunkX the chunk x
     * @param chunkZ the chunk z
     * @return the players viewing the chunk
     */
    default Viewable viewers(int chunkX, int chunkZ) {
        return viewers(Area.chunk(dimensionType(), chunkX, chunkZ));
    }

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
     * Gets the entities located in the worldView.
     *
     * @param area the worldView to get the entities from
     * @return an unmodifiable {@link Set} containing all the entities in a chunk,
     * if {@code chunk} is unloaded, return an empty {@link Set}
     */
    Collection<Entity> areaEntities(Area area);

    /**
     * Gets nearby entities to the given position.
     *
     * @param point position to look at
     * @param range max range from the given point to collect entities at
     * @return entities that are not further than the specified distance from the transmitted position.
     */
    Collection<Entity> nearbyEntities(Point point, double range);

    @ApiStatus.Experimental
    EntityTracker entityTracker();

    /**
     * Gets the instance unique id.
     *
     * @return the instance unique id
     */
    UUID uniqueId();

    /**
     * Checks if the given position is in the void.
     * @param position the position to check
     * @return true if the position is in the void, false otherwise
     */
    boolean isInVoid(Pos position);

    // Packets
    ChunkDataPacket chunkPacket(int chunkX, int chunkZ);
    TimeUpdatePacket timePacket();

    // Pathfinding...
    IInstanceSpace getInstanceSpace();

    /**
     * Sends a {@link BlockActionPacket} for all the viewers of the specific position.
     *
     * @param blockPosition the block position
     * @param actionId      the action id, depends on the block
     * @param actionParam   the action parameter, depends on the block
     * @see <a href="https://wiki.vg/Protocol#Block_Action">BlockActionPacket</a> for the action id &amp; param
     */
    @Deprecated // in favor of Instance#viewers
    default void sendBlockAction(Point blockPosition, byte actionId, byte actionParam) {
        final Block block = getBlock(blockPosition);
        final Set<Player> viewers = viewers(Area.collection(blockPosition)).getViewers();
        PacketUtils.sendGroupedPacket(viewers, new BlockActionPacket(blockPosition, actionId, actionParam, block));
    }

}
