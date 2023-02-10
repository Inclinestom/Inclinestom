package net.minestom.server.entity.metadata.other;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Metadata;
import org.jetbrains.annotations.NotNull;

public class MagmaCubeMeta extends SlimeMeta {
    public static final byte OFFSET = SlimeMeta.MAX_OFFSET;
    public static final byte MAX_OFFSET = OFFSET + 0;

    public MagmaCubeMeta(Entity entity, Metadata metadata) {
        super(entity, metadata);
    }

}
