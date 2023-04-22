package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

final class EntityStorageImpl implements EntityStorage {

    private final Map<Entity, Point> entities = new ConcurrentHashMap<>();

    @Override
    public boolean add(Entity entity, Point point) {
        return this.entities.putIfAbsent(entity, point) == null;
    }

    @Override
    public boolean remove(Entity entity) {
        return this.entities.remove(entity) != null;
    }

    @Override
    public void move(Entity entity, Point newPoint) {
        this.entities.put(entity, newPoint);
    }

    @Override
    public @UnmodifiableView <T extends Entity> Collection<T> entitiesInArea(Area area, Target<T> target) {
        return new AbstractCollection<>() {
            @Override
            public Iterator<T> iterator() {
                return entities.entrySet()
                        .stream()
                        .filter(entry -> target.test(entry.getKey()))
                        .filter(entity -> area.contains(entity.getValue()))
                        .map(entity -> (T) entity)
                        .iterator();
            }

            @Override
            public int size() {
                return (int) entities.entrySet()
                        .stream()
                        .filter(entry -> target.test(entry.getKey()))
                        .filter(entity -> area.contains(entity.getValue()))
                        .count();
            }
        };
    }

    @Override
    public @UnmodifiableView <T extends Entity> Set<T> entities(Target<T> target) {
        //noinspection unchecked
        return this.entities.keySet()
                .stream()
                .filter(target::test)
                .map(entity -> (T) entity)
                .collect(Collectors.toUnmodifiableSet());
    }
}
