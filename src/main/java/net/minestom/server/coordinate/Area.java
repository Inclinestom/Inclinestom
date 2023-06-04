package net.minestom.server.coordinate;

import net.minestom.server.instance.Instance;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.world.DimensionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
     * Creates a new sphere area from a position and a range.
     * @param position the center of the sphere
     * @param range the radius of the sphere
     * @return a new area
     */
    static Area sphere(Point position, double range) {
        position = Vec.fromPoint(position); // This is used to prevent the `position` being a `Pos`.
        int min = (int) -Math.floor(range);
        int max = (int) Math.ceil(range);
        Stream.Builder<Point> builder = Stream.builder();
        for (int x = min; x <= max; x++) {
            for (int y = min; y <= max; y++) {
                for (int z = min; z <= max; z++) {
                    Point point = position.add(x, y, z);
                    if (point.distanceSquared(position) <= range * range) {
                        builder.add(point);
                    }
                }
            }
        }
        return Area.collection(builder.build().toList());
    }

    /**
     * Optimizes the given area's geometry.
     * @param area the unoptimized area
     * @return a new optimized area
     */
    static Area optimize(Area area) {
        // Double invert should optimize the area somewhat, however TODO: look for better optimize solution
        return Area.invert(Area.invert(area));
    }

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
    static Area union(Collection<Area> areas) {
        if (areas.isEmpty()) {
            return Area.empty();
        }
        AreaImpl.Fill[] fillArray = areas.stream()
                .flatMap(area -> area.subdivide().stream())
                .map(area -> (AreaImpl.Fill) area)
                .toArray(AreaImpl.Fill[]::new);
        return AreaImpl.safeUnion(fillArray);
    }

    /**
     * Creates a new area from a source area and an area to exclude.
     * @param source the source area
     * @param exclude the area to exclude
     * @return a new area
     */
    static Area exclude(Area source, Area exclude) {
        Area outside = invert(union(source, exclude));
        Area excluded = invert(union(outside, exclude));
        return excluded.size() == 0 ? Area.empty() : excluded;
    }

    static Area intersection(Area... areas) {
        return Stream.of(areas)
                .reduce(Area::overlap)
                .orElse(Area.empty());
    }

    // Arbitrary sizes

    static Area section(Point sectionPos) {
        return fill(sectionPos, sectionPos.add(Instance.SECTION_SIZE));
    }

    static Area empty() {
        return AreaImpl.EMPTY;
    }

    static Area full() {
        return AreaImpl.FULL;
    }

    static Area chunk(DimensionType dimensionType) {
        return chunk(dimensionType.getMinY(), dimensionType.getMaxY(), 0, 0);
    }

    static Area chunk(DimensionType dimensionType, int chunkX, int chunkZ) {
        return chunk(dimensionType.getMinY(), dimensionType.getMaxY(), chunkX, chunkZ);
    }

    static Area chunk(int minY, int maxY, int chunkX, int chunkZ) {
        Point chunkMin = new Vec(chunkX * Instance.SECTION_SIZE, minY, chunkZ * Instance.SECTION_SIZE);
        Point chunkMax = chunkMin.add(Instance.SECTION_SIZE, 0, Instance.SECTION_SIZE).withY(maxY);
        return fill(chunkMin, chunkMax);
    }

    static Area chunkRange(DimensionType dimensionType, int chunkX, int chunkZ, int radius) {
        int minX = -radius * Instance.SECTION_SIZE;
        int maxX = (radius + 1) * Instance.SECTION_SIZE;
        int minY = dimensionType.getMinY();
        int maxY = dimensionType.getMaxY();
        int minZ = -radius * Instance.SECTION_SIZE;
        int maxZ = (radius + 1) * Instance.SECTION_SIZE;

        int offsetX = chunkX * Instance.SECTION_SIZE;
        int offsetZ = chunkZ * Instance.SECTION_SIZE;

        return fill(new Vec(minX, minY, minZ).add(offsetX, 0, offsetZ), new Vec(maxX, maxY, maxZ).add(offsetX, 0, offsetZ));
    }

    /**
     * Takes an Area and groups its neighboring areas that touch it on any side into connected groups.
     * @param area the area
     * @return a set of connected areas
     */
    static Set<Area> groupConnectedAreas(Area area) {
        return AreaImpl.groupConnectedAreas(area);
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

    /**
     * Translates this area by the given vector.
     * @param point the vector
     * @return a new area
     */
    Area translate(Point point);

    /**
     * Subdivides this area into a set of {@link AreaImpl.Fill}s.
     * @return the set of fill areas
     */
    Set<Area> subdivide();

    default Stream<Point> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    static boolean equals(Area areaA, Area areaB) {
        return areaA.overlapCount(areaB) == areaA.size();
    }
}