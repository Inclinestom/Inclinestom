package net.minestom.server.coordinate;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

// TODO: Area specific optimizations
class AreaImpl {

    public static final Area EMPTY = Area.collection();

    static Area fromCollection(Collection<? extends Point> collection) {
        // Detect any nested nxnxn areas, and create them
        Set<Point> points = collection.stream()
                .map(point -> new Vec(point.blockX(), point.blockY(), point.blockZ()))
                .collect(Collectors.toSet());
        return new SetArea(points);
    }

    static Point findMin(Collection<Point> children) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;

        for (Point point : children) {
            minX = Math.min(minX, point.blockX());
            minY = Math.min(minY, point.blockY());
            minZ = Math.min(minZ, point.blockZ());
        }

        return new Vec(minX, minY, minZ);
    }

    static Point findMax(Collection<Point> children) {
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (Point point : children) {
            maxX = Math.max(maxX, point.blockX());
            maxY = Math.max(maxY, point.blockY());
            maxZ = Math.max(maxZ, point.blockZ());
        }

        return new Vec(maxX, maxY, maxZ);
    }

    static long findSize(Collection<Area> children) {
        long total = children.stream().mapToLong(Area::size).sum();
        long overlap = 0;
        for (Area child : children) {
            for (Area other : children) {
                if (child == other) continue;
                overlap += child.overlap(other);
            }
        }
        return total - overlap;
    }

    static Area intersection(Area[] children) {
        if (children.length == 0) {
            throw new IllegalArgumentException("Must have at least one child");
        }
        Set<Point> points = Stream.of(children)
                .flatMap(child -> StreamSupport.stream(child.spliterator(), false))
                .collect(Collectors.toSet());
        return new SetArea(points);
    }

    static Area exclude(Area source, Area exclude) {
        return new ExcludeArea(source, exclude);
    }

    record SetArea(Set<Point> points, Point min, Point max) implements Area {

        public SetArea(Set<Point> points) {
            this(points, findMin(points), findMax(points));

            if (!isFullyConnected()) {
                throw new IllegalArgumentException("Points must be fully connected");
            }
        }

        public boolean isFullyConnected() {
            if (points.size() == 1) return true;
            Set<Point> connected = points.stream()
                .flatMap(point -> Stream.of(
                        point.add(1, 0, 0),
                        point.add(-1, 0, 0),
                        point.add(0, 1, 0),
                        point.add(0, -1, 0),
                        point.add(0, 0, 1),
                        point.add(0, 0, -1)
                ))
                .collect(Collectors.toSet());
            return connected.containsAll(points);
        }

        @NotNull
        @Override
        public Iterator<Point> iterator() {
            return points.iterator();
        }

        @Override
        public boolean contains(Area area) {
            return StreamSupport.stream(area.spliterator(), false)
                    .allMatch(points::contains);
        }

        @Override
        public boolean contains(Point point) {
            return points.contains(point);
        }

        @Override
        public long size() {
            return points.size();
        }

        @Override
        public long overlap(Area other) {
            return StreamSupport.stream(other.spliterator(), false)
                    .filter(points::contains)
                    .count();
        }
    }

    static final class Fill implements Area {
        private final Point min, max;

        Fill(Point pos1, Point pos2) {
            this.min = Vec.fromPoint(pos1);
            this.max = Vec.fromPoint(pos2);
        }

        @Override
        public Point min() {
            return min;
        }

        @Override
        public Point max() {
            return max;
        }

        @Override
        public boolean contains(Area area) {
            if (area.min().blockX() < min.blockX()) return false;
            if (area.min().blockY() < min.blockY()) return false;
            if (area.min().blockZ() < min.blockZ()) return false;

            if (area.max().blockX() > max.blockX()) return false;
            if (area.max().blockY() > max.blockY()) return false;
            if (area.max().blockZ() > max.blockZ()) return false;

            return true;
        }

        @Override
        public boolean contains(Point point) {
            return point.blockX() >= min.blockX() && point.blockX() <= max.blockX()
                    && point.blockY() >= min.blockY() && point.blockY() <= max.blockY()
                    && point.blockZ() >= min.blockZ() && point.blockZ() <= max.blockZ();
        }

        @Override
        public long size() {
            return (long) (max.blockX() - min.blockX()) * (max.blockY() - min.blockY()) * (max.blockZ() - min.blockZ());
        }

        @Override
        public long overlap(Area other) {
            return StreamSupport.stream(other.spliterator(), false)
                    .filter(this::contains)
                    .count();
        }

        @NotNull
        @Override
        public Iterator<Point> iterator() {
            return new Iterator<>() {
                private int x = min.blockX();
                private int y = min.blockY();
                private int z = min.blockZ();

                @Override
                public boolean hasNext() {
                    return x < max.blockX() && y < max.blockY() && z < max.blockZ();
                }

                @Override
                public Point next() {
                    Point point = new Vec(x, y, z);
                    z++;
                    if (z >= max.blockZ()) {
                        z = min.blockZ();
                        y++;
                        if (y >= max.blockY()) {
                            y = min.blockY();
                            x++;
                        }
                    }
                    return point;
                }
            };
        }
    }

    static class Path implements Area.Path {
        private final List<Point> positions = new ArrayList<>();
        private Point currentPosition;

        @Override
        public Area.Path north(double factor) {
            return with(blockPosition -> blockPosition.add(0, 0, -factor));
        }

        @Override
        public Area.Path south(double factor) {
            return with(blockPosition -> blockPosition.add(0, 0, factor));
        }

        @Override
        public Area.Path east(double factor) {
            return with(blockPosition -> blockPosition.add(factor, 0, 0));
        }

        @Override
        public Area.Path west(double factor) {
            return with(blockPosition -> blockPosition.add(-factor, 0, 0));
        }

        @Override
        public Area.Path up(double factor) {
            return with(blockPosition -> blockPosition.add(0, factor, 0));
        }

        @Override
        public Area.Path down(double factor) {
            return with(blockPosition -> blockPosition.add(0, -factor, 0));
        }

        @Override
        public Area end() {
            return fromCollection(positions);
        }

        private Area.Path with(UnaryOperator<Point> operator) {
            this.currentPosition = operator.apply(currentPosition);
            this.positions.add(currentPosition);
            return this;
        }
    }

    record Union(Collection<Area> children, Point min, Point max, long size) implements Area, Area.HasChildren {

        public Union(Collection<Area> children) {
            this(children,
                    findMin(children.stream().map(Area::min).toList()),
                    findMax(children.stream().map(Area::max).toList()),
                    findSize(children));
        }

        @Override
        public @NotNull Iterator<Point> iterator() {
            return new Iterator<>() {
                private final Iterator<Area> areaIterator = children.iterator();
                private Iterator<Point> currentIterator = areaIterator.next().iterator();

                @Override
                public boolean hasNext() {
                    if (currentIterator.hasNext()) {
                        return true;
                    }

                    while (areaIterator.hasNext()) {
                        currentIterator = areaIterator.next().iterator();
                        if (currentIterator.hasNext()) {
                            return true;
                        }
                    }

                    return false;
                }

                @Override
                public Point next() {
                    return currentIterator.next();
                }
            };
        }

        @Override
        public boolean contains(Area area) {
            Area[] areas = Stream.concat(Stream.of(area), children.stream())
                            .toArray(Area[]::new);
            return Area.union(areas).size() == size();
        }

        @Override
        public boolean contains(Point point) {
            return children.stream().anyMatch(area -> area.contains(point));
        }

        @Override
        public long size() {
            return children.stream().mapToLong(Area::size).sum();
        }

        @Override
        public long overlap(Area other) {
            Area[] areas = Stream.concat(Stream.of(other), children.stream())
                    .toArray(Area[]::new);
            long missing = Area.union(areas).size() - size();
            return other.size() - missing;
        }
    }

    record ExcludeArea(Area source, Area exclude, Point min, Point max, long size) implements Area {

        public ExcludeArea(Area source, Area exclude) {
            this(source, exclude,
                    findMin(StreamSupport.stream(source.spliterator(), false)
                            .filter(point -> !exclude.contains(point))
                            .toList()),
                    findMax(StreamSupport.stream(source.spliterator(), false)
                            .filter(point -> !exclude.contains(point))
                            .toList()),
                    findSize(List.of(source, exclude)) - exclude.size()
            );
        }

        @Override
        public boolean contains(Area area) {
            if (Area.intersection(area, exclude()).size() > 0) return false;
            return source().contains(area);
        }

        @Override
        public boolean contains(Point point) {
            if (exclude().contains(point)) return false;
            return source().contains(point);
        }

        @Override
        public long overlap(Area other) {
            return Area.intersection(source(), other).size()
                    - Area.intersection(exclude(), other).size();
        }

        @NotNull
        @Override
        public Iterator<Point> iterator() {
            return new Iterator<>() {
                private final Iterator<Point> sourceIterator = source.iterator();
                private Point next;

                @Override
                public boolean hasNext() {
                    if (next != null) return true;

                    while (sourceIterator.hasNext()) {
                        Point point = sourceIterator.next();
                        if (exclude.contains(point)) continue;
                        next = point;
                        return true;
                    }

                    return false;
                }

                @Override
                public Point next() {
                    Point point = next;
                    next = null;
                    return point;
                }
            };
        }
    }
}