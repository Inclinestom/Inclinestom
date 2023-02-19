package net.minestom.server.instance;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ExperienceOrb;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Defines how {@link Entity entities} are tracked within an {@link Instance instance}.
 * <p>
 * Implementations are expected to be thread-safe.
 */
@ApiStatus.Experimental
public sealed interface EntityTracker permits EntityTrackerImpl {
    static EntityTracker newTracker() {
        return new EntityTrackerImpl();
    }

    /**
     * Register an entity to be tracked.
     */
    <T extends Entity> void register(Entity entity, Point point,
                                     Target<T> target, @Nullable Update<T> update);

    /**
     * Unregister an entity tracking.
     */
    <T extends Entity> void unregister(Entity entity, Target<T> target, @Nullable Update<T> update);

    /**
     * Called every time an entity move, you may want to verify if the new
     * position is in a different chunk.
     */
    <T extends Entity> void move(Entity entity, Point newPoint,
                                 Target<T> target, @Nullable Update<T> update);

    @UnmodifiableView <T extends Entity> Collection<T> chunkEntities(int chunkX, int chunkZ, Target<T> target);

    @UnmodifiableView
    default <T extends Entity> Collection<T> chunkEntities(Point point, Target<T> target) {
        return chunkEntities(point.sectionX(), point.sectionZ(), target);
    }

    /**
     * Gets the entities within a chunk range.
     */
    <T extends Entity> void nearbyEntitiesByWorldViewRange(Point point, int chunkRange,
                                                       Target<T> target, Consumer<T> query);

    /**
     * Gets the entities within a range.
     */
    <T extends Entity> void nearbyEntities(Point point, double range,
                                           Target<T> target, Consumer<T> query);

    /**
     * Gets all the entities tracked by this class.
     */
    @UnmodifiableView
    <T extends Entity> Set<T> entities(Target<T> target);

    @UnmodifiableView
    default Set<Entity> entities() {
        return entities(Target.ENTITIES);
    }

    /**
     * Represents the type of entity you want to retrieve.
     *
     * @param <E> the entity type
     */
    @ApiStatus.NonExtendable
    interface Target<E extends Entity> {
        Target<Entity> ENTITIES = create(Entity.class);
        Target<Player> PLAYERS = create(Player.class);
        Target<ItemEntity> ITEMS = create(ItemEntity.class);
        Target<ExperienceOrb> EXPERIENCE_ORBS = create(ExperienceOrb.class);

        List<EntityTracker.Target<? extends Entity>> TARGETS = List.of(EntityTracker.Target.ENTITIES, EntityTracker.Target.PLAYERS, EntityTracker.Target.ITEMS, EntityTracker.Target.EXPERIENCE_ORBS);

        Class<E> type();

        int ordinal();

        private static <T extends Entity> EntityTracker.Target<T> create(Class<T> type) {
            final int ordinal = EntityTrackerImpl.TARGET_COUNTER.getAndIncrement();
            return new Target<>() {
                @Override
                public Class<T> type() {
                    return type;
                }

                @Override
                public int ordinal() {
                    return ordinal;
                }
            };
        }
    }

    /**
     * Callback to know the newly visible entities and those to remove.
     */
    interface Update<E extends Entity> {
        void add(E entity);

        void remove(E entity);

        default void referenceUpdate(Point point, @Nullable EntityTracker tracker) {
            // Empty
        }
    }
}
