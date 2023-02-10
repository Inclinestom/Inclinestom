package net.minestom.server.event.entity.projectile;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public final class ProjectileCollideWithBlockEvent extends ProjectileCollideEvent {

    private final Block block;

    public ProjectileCollideWithBlockEvent(
            Entity projectile,
            Pos position,
            Block block
    ) {
        super(projectile, position);
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
