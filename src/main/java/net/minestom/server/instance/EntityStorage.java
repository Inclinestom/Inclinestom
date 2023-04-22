package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ExperienceOrb;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Defines how {@link Entity entities} are tracked within an {@link Instance instance}.
 * <p>
 * Implementations are expected to be thread-safe.
 */
@ApiStatus.Experimental
public sealed interface EntityStorage permits EntityStorageImpl {
    static EntityStorage newStorage() {
        return new EntityStorageImpl();
    }

    /**
     * Adds an entity to this tracker.
     * @param entity the entity to add
     * @param point the entity's position
     * @return true if the entity was added, false if it was already tracked
     */
    boolean add(Entity entity, Point point);

    /**
     * Removes an entity from this tracker.
     * @param entity the entity to remove
     * @return true if the entity was removed, false if it was not tracked
     */
    boolean remove(Entity entity);

    /**
     * Called every time an entity moves.
     */
    void move(Entity entity, Point newPoint);

    default @UnmodifiableView Collection<Entity> entitiesInArea(Area area) {
        return entitiesInArea(area, Target.ENTITIES);
    }
    @UnmodifiableView <T extends Entity> Collection<T> entitiesInArea(Area area, Target<T> target);

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
        Target<Entity> ENTITIES = entity -> true;
        Target<Player> PLAYERS = Player.class::isInstance;
        Target<ItemEntity> ITEMS = ItemEntity.class::isInstance;
        Target<ExperienceOrb> EXPERIENCE_ORBS = ExperienceOrb.class::isInstance;

        List<EntityStorage.Target<? extends Entity>> TARGETS = List.of(EntityStorage.Target.ENTITIES, EntityStorage.Target.PLAYERS, EntityStorage.Target.ITEMS, EntityStorage.Target.EXPERIENCE_ORBS);

        boolean test(Entity entity);
    }
}
