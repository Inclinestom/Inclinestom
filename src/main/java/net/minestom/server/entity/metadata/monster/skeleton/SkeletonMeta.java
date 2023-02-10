package net.minestom.server.entity.metadata.monster.skeleton;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Metadata;
import org.jetbrains.annotations.NotNull;

public class SkeletonMeta extends AbstractSkeletonMeta {
    public static final byte OFFSET = AbstractSkeletonMeta.MAX_OFFSET;
    public static final byte MAX_OFFSET = OFFSET + 0;

    public SkeletonMeta(Entity entity, Metadata metadata) {
        super(entity, metadata);
    }

}
