package net.minestom.server.utils.entity;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.storage.WorldView;
import net.minestom.server.instance.block.Block;

public final class EntityUtils {

    private EntityUtils() {
    }

    public static boolean isOnGround(Entity entity) {
        final WorldView view = entity.getInstance().worldView();
        if (view == null)
            return false;
        final Pos entityPosition = entity.getPosition();
        // TODO: check entire bounding box
        try {
            return view.getBlock(entityPosition.sub(0, 1, 0)).isSolid();
        } catch (NullPointerException e) {
            // Probably an entity at the border of an unloaded chunk
            return false;
        }
    }
}
