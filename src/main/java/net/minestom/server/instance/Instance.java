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
     * Implementations of this method may load additional chunks beyond the bounds of the given area.
     * Note that while the method may load more than just the given area, the resulting {@link WorldView} will always be the
     * size of the given area.
     *
     * @param area the area to load
     * @return a {@link CompletableFuture} that completes when the area is loaded or fails if loading fails
     */
    CompletableFuture<WorldView> loadArea(Area area);

    /**
     * Unloads the {@link WorldView} covering the given {@link Area}.
     * Implementations may unload more than the given area, but must unload at least the given area.
     *
     * @param area the area to unload
     * @return a {@link CompletableFuture} that completes when the area is unloaded
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
     * Gets the {@link Area} which contains all loaded world data.
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
     * Sets the logic to use when deciding which areas of the world should be loaded.
     * @param loadingRule the new worldView load rule
     * @see PlayerRadiusLoadingRule
     */
    void setLoadingRule(LoadingRule loadingRule);

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
     * NOTE: Generally you should use Entity#setInstance instead.
     * Adds the given entity to the instance.
     * @param entity the entity to add
     * @return true if the entity has been added, false otherwise.
     */
    @ApiStatus.Internal
    CompletableFuture<Boolean> addEntity(@NotNull Entity entity, @NotNull Point spawnPosition);

    /**
     * NOTE: Generally you should use Entity#setInstance instead.
     * Removes the given entity from the instance.
     * @param entity the entity to remove
     * @return true if the entity has been removed, false otherwise.
     */
    @ApiStatus.Internal
    CompletableFuture<Boolean> removeEntity(@NotNull Entity entity);

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
    EntityStorage entityTracker();

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

    // Packets (commonly subject to change)
    Collection<ChunkDataPacket> chunkPackets(Area area);

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
