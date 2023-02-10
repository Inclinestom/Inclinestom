package net.minestom.server.entity.metadata.monster.raider;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Metadata;
import org.jetbrains.annotations.NotNull;

public class RavagerMeta extends RaiderMeta {
    public static final byte OFFSET = RaiderMeta.MAX_OFFSET;
    public static final byte MAX_OFFSET = OFFSET + 0;

    public RavagerMeta(Entity entity, Metadata metadata) {
        super(entity, metadata);
    }

}
