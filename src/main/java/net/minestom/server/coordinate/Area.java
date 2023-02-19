package net.minestom.server.coordinate;

import net.minestom.server.instance.Instance;
import net.minestom.server.world.DimensionType;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An area is a spatially connected set of block positions.
 * These areas can be used for optimizations such as instance block queries, and pathfinding domains.
 */
public sealed interface Area extends Iterable<Point> permits AreaImpl.Fill, AreaImpl.FillUnion {

    // Primitives

    /**
     * Creates a new rectangular prism area from two points.
     * @param point1 the first (min) point
     * @param point2 the second (max) point
     * @return a new area
     */
    static Area fill(Point point1, Point point2) {
        return AreaImpl.fill(point1, point2);
    }

    /**
     * Creates a union of two given areas.
     * @param areaA the first area
     * @param areaB the second area
     * @return a new area
     */
    static Area union(Area areaA, Area areaB) {
        return AreaImpl.union(areaA, areaB);
    }

    // Structures

    /**
     * Creates a new area from an inverted source area.
     * @param source the source area
     * @return a new area
     */
    static Area invert(Area source) {
        return AreaImpl.invert(source);
    }

    /**
     * Creates a new area from a collection of block positions. Note that these points will be block-aligned.
     * @param collection the collection of block positions
     * @return a new area
     * @throws IllegalStateException if the resulting area is not fully connected
     */
    static Area collection(Collection<? extends Point> collection) {
        return Area.union(collection.stream().map(Area::block).toList());
    }

    /**
     * Creates a new area from a collection of block positions. Note that these points will be block-aligned.
     * @param collection the collection of block positions
     * @return a new area
     * @throws IllegalStateException if the resulting area is not fully connected
     */
    static Area collection(Point... collection) {
        return collection(List.of(collection));
    }

    /**
     * Creates a new 1x1 area from the given point.
     * @param point the point
     * @return a new area
     */
    static Area block(Point point) {
        return fill(point, point.add(1));
    }

    /**
     * Creates a union of multiple areas.
     * @param areas the areas to union
     * @return a new area
     */
    static Area union(Area... areas) {
        return union(List.of(areas));
    }

    /**
     * Creates a union of multiple areas.
     * @param areas the areas to union
     * @return a new area
     */
    static Area union(List<Area> areas) {
        return areas.stream()
                .reduce(Area::union)
                .orElse(Area.empty());
    }

    /**
     * Creates a new area from a source area and an area to exclude.
     * @param source the source area
     * @param exclude the area to exclude
     * @return a new area
     */
    static Area exclude(Area source, Area exclude) {
        Area outside = invert(union(source, exclude));
        return invert(union(outside, exclude));
    }

    static Area intersection(Area... areas) {
        return Stream.of(areas)
                .reduce(Area::intersection)
                .orElse(Area.empty());
    }

    static Area intersection(Area areaA, Area areaB) {
        return AreaImpl.intersection(areaA, areaB);
    }

    // Arbitrary sizes

    static Area section(Vec sectionPos) {
        return fill(sectionPos, sectionPos.add(Instance.SECTION_SIZE));
    }

    static Area empty() {
        return AreaImpl.EMPTY;
    }

    static Area full() {
        return AreaImpl.FULL;
    }

    static Area chunk(DimensionType dimensionType, int chunkX, int chunkZ) {
        Point chunkMin = new Vec(chunkX * Instance.SECTION_SIZE, dimensionType.getMinY(), chunkZ * Instance.SECTION_SIZE);
        Point chunkMax = chunkMin.add(Instance.SECTION_SIZE, 0, Instance.SECTION_SIZE).withY(dimensionType.getMaxY());
        return fill(chunkMin, chunkMax);
    }

    /**
     * The minimum point of this area
     * @return the minimum point
     */
    Point min();

    /**
     * The maximum point of this area
     * @return the maximum point
     */
    Point max();

    /**
     * Checks if the given area is within this area.
     * @param area the area
     * @return true if the area is within this area
     */
    boolean contains(Area area);

    /**
     * Checks if the given point is within this area.
     * @param point the point
     * @return true if the point is within this area
     */
    boolean contains(Point point);

    /**
     * Checks if the given coordinates are within this area.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return true if the point is within this area
     */
    boolean contains(double x, double y, double z);

    /**
     * @return the number of points in this area
     */
    long size();

    /**
     * Calculates the overlapping area between this area and another.
     * @param other the other area
     * @return the overlapping area
     */
    Area overlap(Area other);

    /**
     * Calculates the count of points in the overlapping area between this area and another.
     * @param other the other area
     * @return the overlapping area
     */
    long overlapCount(Area other);

    /**
     * Checks if the given area overlaps with this area.
     * @param other the other area
     * @return true if the areas overlap
     */
    boolean overlaps(Area other);

    default Stream<Point> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}