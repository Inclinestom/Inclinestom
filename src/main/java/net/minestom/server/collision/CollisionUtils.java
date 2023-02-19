package net.minestom.server.collision;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.storage.WorldView;
import net.minestom.server.registry.Registry;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
@ApiStatus.Experimental
public final class CollisionUtils {

    /**
     * Moves an entity with physics applied (ie checking against blocks)
     * <p>
     * Works by getting all the full blocks that an entity could interact with.
     * All bounding boxes inside the full blocks are checked for collisions with the entity.
     *
     * @param entity the entity to move
     * @return the result of physics simulation
     */
    public static PhysicsResult handlePhysics(Entity entity, Vec entityVelocity,
                                              @Nullable PhysicsResult lastPhysicsResult) {
        final Instance instance = entity.getInstance();
        assert instance != null;
        return handlePhysics(instance, entity.getWorldView(),
                entity.getBoundingBox(),
                entity.getPosition(), entityVelocity,
                lastPhysicsResult);
    }

    /**
     * Moves bounding box with physics applied (ie checking against blocks)
     * <p>
     * Works by getting all the full blocks that a bounding box could interact with.
     * All bounding boxes inside the full blocks are checked for collisions with the given bounding box.
     *
     * @param boundingBox the bounding box to move
     * @return the result of physics simulation
     */
    public static PhysicsResult handlePhysics(Instance instance, @Nullable WorldView chunk,
                                              BoundingBox boundingBox,
                                              Pos position, Vec velocity,
                                              @Nullable PhysicsResult lastPhysicsResult) {
        return BlockCollision.handlePhysics(boundingBox,
                velocity, position,
                instance.worldView(), lastPhysicsResult);
    }

    /**
     * Checks whether shape is reachable by the given line of sight
     * (ie there are no blocks colliding with it).
     *
     * @param instance the instance.
     * @param chunk    optional chunk reference for speedup purposes.
     * @param start    start of the line of sight.
     * @param end      end of the line of sight.
     * @param shape    shape to check.
     * @return true is shape is reachable by the given line of sight; false otherwise.
     */
    public static boolean isLineOfSightReachingShape(Instance instance, @Nullable WorldView chunk,
                                                     Point start, Point end,
                                                     Shape shape) {
        final PhysicsResult result = handlePhysics(instance, chunk,
                BoundingBox.ZERO,
                Pos.fromPoint(start), Vec.fromPoint(end.sub(start)),
                null);
        return shape.intersectBox(end.sub(result.newPosition()).sub(Vec.EPSILON), BoundingBox.ZERO);
    }

    public static PhysicsResult handlePhysics(Entity entity, Vec entityVelocity) {
        return handlePhysics(entity, entityVelocity, null);
    }

    public static Entity canPlaceBlockAt(Instance instance, Point blockPos, Block b) {
        return BlockCollision.canPlaceBlockAt(instance, blockPos, b);
    }

    /**
     * Applies world border collision.
     *
     * @param instance        the instance where the world border is
     * @param currentPosition the current position
     * @param newPosition     the future target position
     * @return the position with the world border collision applied (can be {@code newPosition} if not changed)
     */
    public static Pos applyWorldBorder(Instance instance,
                                                Pos currentPosition, Pos newPosition) {
        final WorldBorder worldBorder = instance.worldBorder();
        final WorldBorder.CollisionAxis collisionAxis = worldBorder.getCollisionAxis(newPosition);
        return switch (collisionAxis) {
            case NONE ->
                // Apply velocity + gravity
                    newPosition;
            case BOTH ->
                // Apply Y velocity/gravity
                    new Pos(currentPosition.x(), newPosition.y(), currentPosition.z());
            case X ->
                // Apply Y/Z velocity/gravity
                    new Pos(currentPosition.x(), newPosition.y(), newPosition.z());
            case Z ->
                // Apply X/Y velocity/gravity
                    new Pos(newPosition.x(), newPosition.y(), currentPosition.z());
        };
    }

    public static Shape parseBlockShape(String str, Registry.BlockEntry blockEntry) {
        return ShapeImpl.parseBlockFromRegistry(str, blockEntry);
    }
}
