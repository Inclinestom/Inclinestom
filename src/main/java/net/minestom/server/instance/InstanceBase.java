package net.minestom.server.instance;

import it.unimi.dsi.fastutil.ints.*;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointers;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ExperienceOrb;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.pathfinding.PFInstanceSpace;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ChunkDataPacket;
import net.minestom.server.network.packet.server.play.TimeUpdatePacket;
import net.minestom.server.network.packet.server.play.UnloadChunkPacket;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.utils.AreaUtils;
import net.minestom.server.utils.PacketUtils;
import net.minestom.server.utils.async.AsyncUtils;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.utils.time.Cooldown;
import net.minestom.server.utils.time.TimeUnit;
import net.minestom.server.utils.validate.Check;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Instances are what are called "worlds" in Minecraft
 * <p>
 * An instance has entities, blocks, biomes, and lighting.
 */
public abstract class InstanceBase implements Instance {


    protected final DimensionType dimensionType;

    protected final WorldBorder worldBorder;

    // Tick since the creation of the instance
    protected long worldAge;

    // The time of the instance
    protected long time;
    protected int timeRate = 1;
    protected Duration timeUpdate = Duration.of(1, TimeUnit.SECOND);
    protected long lastTimeUpdate;

    // Field for tick events
    protected long lastTickAge = System.currentTimeMillis();

    protected final EntityStorage entityStorage = new EntityStorageImpl();
    protected final Map<Player, Area> player2LoadedSections = Collections.synchronizedMap(new WeakHashMap<>());

    // the uuid of this instance
    protected UUID uniqueId;

    // instance custom data
    protected final TagHandler tagHandler = TagHandler.newHandler();
    protected final Scheduler scheduler = Scheduler.newScheduler();
    protected final EventNode<InstanceEvent> eventNode;

    // Pathfinder
    protected final PFInstanceSpace instanceSpace = new PFInstanceSpace(this);

    // Adventure
    protected final Pointers pointers;

    /**
     * Creates a new instance.
     *
     * @param uniqueId      the {@link UUID} of the instance
     * @param dimensionType the {@link DimensionType} of the instance
     */
    public InstanceBase(UUID uniqueId, DimensionType dimensionType) {
        Check.argCondition(!dimensionType.isRegistered(),
                "The dimension " + dimensionType.getName() + " is not registered! Please use DimensionTypeManager#addDimension");
        this.uniqueId = uniqueId;
        this.dimensionType = dimensionType;

        this.worldBorder = new WorldBorder(this);

        this.pointers = Pointers.builder()
                .withDynamic(Identity.UUID, this::getUniqueId)
                .build();

        final ServerProcess process = MinecraftServer.process();
        if (process != null) {
            this.eventNode = process.eventHandler().map(this, EventFilter.INSTANCE);
        } else {
            // Local nodes require a server process
            this.eventNode = null;
        }
    }

    /**
     * Gets the instance {@link DimensionType}.
     *
     * @return the dimension of the instance
     */
    public DimensionType getDimensionType() {
        return dimensionType;
    }

    /**
     * Gets the age of this instance in tick.
     *
     * @return the age of this instance in tick
     */
    public long getWorldAge() {
        return worldAge;
    }

    /**
     * Gets the current time in the instance (sun/moon).
     *
     * @return the time in the instance
     */
    public long getTime() {
        return time;
    }

    /**
     * Changes the current time in the instance, from 0 to 24000.
     * <p>
     * If the time is negative, the vanilla client will not move the sun.
     * <p>
     * 0 = sunrise
     * 6000 = noon
     * 12000 = sunset
     * 18000 = midnight
     * <p>
     * This method is unaffected by {@link #getTimeRate()}
     * <p>
     * It does send the new time to all players in the instance, unaffected by {@link #getTimeUpdate()}
     *
     * @param time the new time of the instance
     */
    public void setTime(long time) {
        this.time = time;
        PacketUtils.sendGroupedPacket(getPlayers(), createTimePacket());
    }

    /**
     * Gets the rate of the time passing, it is 1 by default
     *
     * @return the time rate of the instance
     */
    public int getTimeRate() {
        return timeRate;
    }

    /**
     * Changes the time rate of the instance
     * <p>
     * 1 is the default value and can be set to 0 to be completely disabled (constant time)
     *
     * @param timeRate the new time rate of the instance
     * @throws IllegalStateException if {@code timeRate} is lower than 0
     */
    public void setTimeRate(int timeRate) {
        Check.stateCondition(timeRate < 0, "The time rate cannot be lower than 0");
        this.timeRate = timeRate;
    }

    /**
     * Gets the rate at which the client is updated with the current instance time
     *
     * @return the client update rate for time related packet
     */
    public @Nullable Duration getTimeUpdate() {
        return timeUpdate;
    }

    /**
     * Changes the rate at which the client is updated about the time
     * <p>
     * Setting it to null means that the client will never know about time change
     * (but will still change server-side)
     *
     * @param timeUpdate the new update rate concerning time
     */
    public void setTimeUpdate(@Nullable Duration timeUpdate) {
        this.timeUpdate = timeUpdate;
    }

    /**
     * Creates a {@link TimeUpdatePacket} with the current age and time of this instance
     *
     * @return the {@link TimeUpdatePacket} with this instance data
     */
    @ApiStatus.Internal
    public TimeUpdatePacket createTimePacket() {
        long time = this.time;
        if (timeRate == 0) {
            //Negative values stop the sun and moon from moving
            //0 as a long cannot be negative
            time = time == 0 ? -24000L : -Math.abs(time);
        }
        return new TimeUpdatePacket(worldAge, time);
    }

    /**
     * Gets the instance {@link WorldBorder};
     *
     * @return the {@link WorldBorder} linked to the instance
     */
    public WorldBorder getWorldBorder() {
        return worldBorder;
    }

    /**
     * Gets the entities in the instance;
     *
     * @return an unmodifiable {@link Set} containing all the entities in the instance
     */
    public Set<Entity> getEntities() {
        return entityStorage.entities();
    }

    /**
     * Gets the players in the instance;
     *
     * @return an unmodifiable {@link Set} containing all the players in the instance
     */
    @Override
    public Set<Player> getPlayers() {
        return entityStorage.entities(EntityStorage.Target.PLAYERS);
    }

    /**
     * Gets the creatures in the instance;
     *
     * @return an unmodifiable {@link Set} containing all the creatures in the instance
     */
    @Deprecated
    public Set<EntityCreature> getCreatures() {
        return entityStorage.entities().stream()
                .filter(EntityCreature.class::isInstance)
                .map(entity -> (EntityCreature) entity)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Gets the experience orbs in the instance.
     *
     * @return an unmodifiable {@link Set} containing all the experience orbs in the instance
     */
    @Deprecated
    public Set<ExperienceOrb> getExperienceOrbs() {
        return entityStorage.entities().stream()
                .filter(ExperienceOrb.class::isInstance)
                .map(entity -> (ExperienceOrb) entity)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Gets nearby entities to the given position.
     *
     * @param point position to look at
     * @param range max range from the given point to collect entities at
     * @return entities that are not further than the specified distance from the transmitted position.
     */
    public Collection<Entity> getNearbyEntities(Point point, double range) {
        Area area = Area.sphere(point, range);
        return List.copyOf(this.entityStorage.entitiesInArea(area, EntityStorage.Target.ENTITIES));
    }

    @Override
    public abstract @Nullable Block getBlock(int x, int y, int z, Condition condition);

    @ApiStatus.Experimental
    public EntityStorage entityTracker() {
        return entityStorage;
    }

    /**
     * Gets the instance unique id.
     *
     * @return the instance unique id
     */
    public UUID getUniqueId() {
        return uniqueId;
    }

    /**
     * Performs a single tick in the instance
     * <p>
     * Warning: this does not update chunks and entities.
     *
     * @param time the tick time in milliseconds
     */
    @Override
    public CompletableFuture<Void> tick(long time) {
        // Scheduled tasks
        this.scheduler.processTick();

        // Update player chunks
        Area totalChunks = Area.union(players().stream().map(Player::viewArea).toList());
        loadArea(totalChunks).join();
        players().forEach(player -> {
            Area loadedSections = player2LoadedSections.computeIfAbsent(player, p -> Area.empty());

            Area newSections = Area.exclude(player.viewArea(), loadedSections);
            Area oldSections = Area.exclude(loadedSections, player.viewArea());

            AreaUtils.forEachChunk(newSections, (x, z) -> {
                ChunkDataPacket packet = chunkPacket(x, z);
                player.sendPacket(packet);
            });

            AreaUtils.forEachChunk(oldSections, (x, z) -> {
                UnloadChunkPacket packet = new UnloadChunkPacket(x, z);
                player.sendPacket(packet);
            });

            player2LoadedSections.put(player, player.viewArea());
        });

        // blocks + entities
        tickBlocks(time).join();
        tickEntities(time).join();

        // Time
        {
            this.worldAge++;
            this.time += timeRate;
            // time needs to be sent to players
            if (timeUpdate != null && !Cooldown.hasCooldown(time, lastTimeUpdate, timeUpdate)) {
                PacketUtils.sendGroupedPacket(getPlayers(), createTimePacket());
                this.lastTimeUpdate = time;
            }

        }
        // Tick event
        {
            // Process tick events
            EventDispatcher.call(new InstanceTickEvent(this, time, lastTickAge));
            // Set last tick age
            this.lastTickAge = time;
        }
        this.worldBorder.update();
        return AsyncUtils.VOID_FUTURE;
    }

    protected abstract CompletableFuture<Void> tickBlocks(long time);
    protected abstract CompletableFuture<Void> tickEntities(long time);

    @Override
    public TagHandler tagHandler() {
        return tagHandler;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    @ApiStatus.Experimental
    public EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    /**
     * Gets the instance space.
     * <p>
     * Used by the pathfinder for entities.
     *
     * @return the instance space
     */
    @ApiStatus.Internal
    public PFInstanceSpace getInstanceSpace() {
        return instanceSpace;
    }

    @Override
    public Pointers pointers() {
        return this.pointers;
    }

    @Override
    public DimensionType dimensionType() {
        return this.dimensionType;
    }

    @Override
    public WorldBorder worldBorder() {
        return this.worldBorder;
    }

    @Override
    public UUID uniqueId() {
        return this.uniqueId;
    }

    @Override
    public TimeUpdatePacket timePacket() {
        return new TimeUpdatePacket(this.worldAge, this.time);
    }

    @Override
    public boolean isInVoid(Pos position) {
        return position.y() < this.dimensionType().getMinY();
    }

    @Override
    public void refreshArea(Player player, Area area) {
        AreaUtils.forEachChunk(area, (x, z) -> {
            ChunkDataPacket packet = chunkPacket(x, z);
            player.sendPacket(packet);
        });
    }
}
