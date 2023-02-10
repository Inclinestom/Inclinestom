package net.minestom.server.coordinate;

import net.minestom.server.instance.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * An area is a spatially connected set of block positions.
 * These areas can be used for optimizations such as instance block queries, and pathfinding domains.
 */
public sealed interface Area extends Iterable<Point> permits AreaImpl.ExcludeArea, AreaImpl.Fill, AreaImpl.SetArea, AreaImpl.Union {

    /**
     * Creates a new area from a collection of block positions. Note that these points will be block-aligned.
     * @param collection the collection of block positions
     * @return a new area
     * @throws IllegalStateException if the resulting area is not fully connected
     */
    static Area collection(Collection<? extends Point> collection) {
        return AreaImpl.fromCollection(collection);
    }

    /**
     * Creates a new area from a collection of block positions. Note that these points will be block-aligned.
     * @param collection the collection of block positions
     * @return a new area
     * @throws IllegalStateException if the resulting area is not fully connected
     */
    static Area collection(Iterable<? extends Point> collection) {
        return AreaImpl.fromCollection(StreamSupport.stream(collection.spliterator(), false).toList());
    }

    /**
     * Creates a new area from a collection of block positions. Note that these points will be block-aligned.
     * @param collection the collection of block positions
     * @return a new area
     * @throws IllegalStateException if the resulting area is not fully connected
     */
    static Area collection(Point... collection) {
        return AreaImpl.fromCollection(List.of(collection));
    }

    /**
     * Creates a new rectangular prism area from two points.
     * @param point1 the first (min) point
     * @param point2 the second (max) point
     * @return a new area
     */
    static Area fill(Point point1, Point point2) {
        return new AreaImpl.Fill(point1, point2);
    }

    /**
     * Creates a union of multiple areas.
     * @param areas the areas to union
     * @return a new area
     */
    static Area union(Area... areas) {
        return new AreaImpl.Union(List.of(areas));
    }

    /**
     * Creates an intersection of multiple areas.
     * @param areas the areas to intersect
     * @return a new area
     */
    static Area intersection(Area... areas) {
        return AreaImpl.intersection(areas);
    }

    /**
     * Creates a new area from a source area and an area to exclude.
     * @param source the source area
     * @param exclude the area to exclude
     * @return a new area
     */
    static Area exclude(Area source, Area exclude) {
        return AreaImpl.exclude(source, exclude);
    }

    /**
     * Starts a path pointer used to construct an area. This is useful for pathfinding purposes.
     * @return a new path pointer
     */
    static Area.Path path() {
        return new AreaImpl.Path();
    }

    static Area section(Vec sectionIndex) {
        return fill(sectionIndex, sectionIndex.add(Chunk.CHUNK_SECTION_SIZE));
    }

    static Area empty() {
        return AreaImpl.EMPTY;
    }

    static Area full() {
        return fill(new Vec(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
                new Vec(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
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
     * @return the number of points in this area
     */
    long size();

    /**
     * Calculates the number of overlapping points between this area and another.
     * @param other the other area
     * @return the number of overlapping points
     */
    long overlap(Area other);

    interface Path {
        Area.Path north(double factor);

        Area.Path south(double factor);

        Area.Path east(double factor);

        Area.Path west(double factor);

        Area.Path up(double factor);

        Area.Path down(double factor);

        Area end();

        default Area.Path north() {
            return north(1);
        }

        default Area.Path south() {
            return south(1);
        }

        default Area.Path east() {
            return east(1);
        }

        default Area.Path west() {
            return west(1);
        }

        default Area.Path up() {
            return up(1);
        }

        default Area.Path down() {
            return down(1);
        }
    }

    sealed interface HasChildren permits AreaImpl.Union {
        Collection<Area> children();
    }
}