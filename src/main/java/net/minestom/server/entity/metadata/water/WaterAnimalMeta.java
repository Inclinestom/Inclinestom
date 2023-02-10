package net.minestom.server.entity.metadata.water;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.metadata.PathfinderMobMeta;
import org.jetbrains.annotations.NotNull;

public class WaterAnimalMeta extends PathfinderMobMeta {
    public static final byte OFFSET = PathfinderMobMeta.MAX_OFFSET;
    public static final byte MAX_OFFSET = OFFSET + 0;

    protected WaterAnimalMeta(Entity entity, Metadata metadata) {
        super(entity, metadata);
    }

}
