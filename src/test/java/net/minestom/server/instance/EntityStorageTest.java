package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.world.DimensionType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EntityStorageTest {
    @Test
    public void register() {
        var ent1 = new Entity(EntityType.ZOMBIE);
        EntityStorage tracker = EntityStorage.newStorage();
        Area chunk = Area.chunk(DimensionType.OVERWORLD, 0, 0);
        var chunkEntities = tracker.entitiesInArea(chunk, EntityStorage.Target.ENTITIES);
        assertTrue(chunkEntities.isEmpty());

        tracker.add(ent1, Vec.ZERO);
        assertEquals(1, chunkEntities.size());

        tracker.remove(ent1);
        assertEquals(0, chunkEntities.size());
    }

    @Test
    public void move() {
        var ent1 = new Entity(EntityType.ZOMBIE);

        EntityStorage storage = EntityStorage.newStorage();

        storage.add(ent1, Vec.ZERO);
        assertEquals(1, storage.entities().size());

        storage.move(ent1, new Vec(32, 0, 32));
        assertEquals(0, storage.entitiesInArea(Area.block(Vec.ZERO)).size());
        assertEquals(1, storage.entitiesInArea(Area.block(new Vec(32, 0, 32))).size());
    }
}
