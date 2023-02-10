package net.minestom.server.entity.metadata.other;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.metadata.EntityMeta;
import org.jetbrains.annotations.NotNull;

public class MarkerMeta extends EntityMeta {
    public static final byte OFFSET = EntityMeta.MAX_OFFSET;
    public static final byte MAX_OFFSET = OFFSET + 0;

    public MarkerMeta(Entity entity, Metadata metadata) {
        super(entity, metadata);
    }
}
