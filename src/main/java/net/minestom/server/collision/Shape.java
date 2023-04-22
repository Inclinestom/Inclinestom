package net.minestom.server.collision;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Experimental
public interface Shape {
    /**
     * Checks if two bounding boxes intersect.
     *
     * @param positionRelative Relative position of bounding box to check with
     * @param boundingBox      Bounding box to check for intersections with
     * @return is an intersection found
     */
    boolean intersectBox(Point positionRelative, BoundingBox boundingBox);

    /**
     * Checks if a moving bounding box will hit this shape.
     *
     * @param rayStart     Position of the moving shape
     * @param rayDirection Movement vector
     * @param shapePos     Position of this shape
     * @param moving       Bounding Box of moving shape
     * @param finalResult  Stores final SweepResult
     * @return is an intersection found
     */
    boolean intersectBoxSwept(Point rayStart, Point rayDirection,
                              Point shapePos, BoundingBox moving, SweepResult finalResult);

    /**
     * Relative Start
     *
     * @return Start of shape
     */
    Point relativeStart();

    /**
     * Relative End
     *
     * @return End of shape
     */
    Point relativeEnd();

    /**
     * To Area
     *
     * @return Area of shape
     */
    Area toArea();
}
