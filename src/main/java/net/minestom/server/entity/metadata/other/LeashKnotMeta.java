package net.minestom.server.entity.metadata.other;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.metadata.EntityMeta;
import org.jetbrains.annotations.NotNull;

public class LeashKnotMeta extends EntityMeta {
    public static final byte OFFSET = EntityMeta.MAX_OFFSET;
    public static final byte MAX_OFFSET = OFFSET + 0;

    public LeashKnotMeta(Entity entity, Metadata metadata) {
        super(entity, metadata);
    }

}
