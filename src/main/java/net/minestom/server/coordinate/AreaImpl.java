package net.minestom.server.coordinate;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

interface AreaImpl {



    Fill EMPTY = new Fill(Vec.ZERO, Vec.ZERO);
    Fill FULL = new Fill(new Vec(Integer.MIN_VALUE), new Vec(Integer.MAX_VALUE));

    static Fill fill(Point point1, Point point2) {
        Point min = findMin(point1, point2);
        Point max = findMax(point1, point2);
        if (min.blockX() == max.blockX() || min.blockY() == max.blockY() || min.blockZ() == max.blockZ()) {
            return EMPTY;
        }
        return new Fill(min, max);
    }

    static Area union(Area areaA, Area areaB) {
        if (areaA instanceof Fill fillA) {
            if (areaB instanceof Fill fillB) {
                return safeUnion(fillA, fillB);
            } else if (areaB instanceof FillUnion unionB) {
                return union(unionB, fillA);
            }
        }
        if (areaA instanceof FillUnion unionA) {
            if (areaB instanceof Fill fillB) {
                return safeUnion(Stream.concat(Stream.of(unionA.areas), Stream.of(fillB)).toArray(Fill[]::new));
            } else if (areaB instanceof FillUnion unionB) {
                return safeUnion(Stream.concat(Stream.of(unionA.areas), Stream.of(unionB.areas)).toArray(Fill[]::new));
            }
        }
        throw new IllegalArgumentException("Unknown area type for union: " + areaA.getClass().getName() + " and " + areaB.getClass().getName());
    }

    private static FillUnion safeUnion(Fill... fills) {
        // Remove equal areas
        fills = Arrays.stream(fills).distinct().toArray(Fill[]::new);

        if (!overlaps(fills)) { // safely doesn't overlap
            return new FillUnion(fills);
        }

        // We need to remove an overlap

        // Find the overlap
        Fill overlapAreaA = null;
        Fill overlapAreaB = null;
        for (Fill areaA : fills) {
            if (overlapAreaA != null) break;
            for (Fill fill : fills) {
                if (areaA.equals(fill)) continue;
                if (areaA.overlaps(fill)) {
                    overlapAreaA = areaA;
                    overlapAreaB = fill;
                    break;
                }
            }
        }

        if (overlapAreaA == null || overlapAreaB == null) {
            throw new IllegalStateException("Failed to find an overlap in " + Arrays.toString(fills));
        }

        // Remove the overlap
        List<Fill> buffer = new ArrayList<>(List.of(fills));
        buffer.remove(overlapAreaA);

        // Add the replacements
        overlapAreaA.fillsRemove(overlapAreaB, buffer::add);

        // Reattempt to create the union
        return safeUnion(buffer.toArray(Fill[]::new));
    }

    private static boolean overlaps(Fill... fills) {
        for (Fill fillA : fills) {
            for (Fill fillB : fills) {
                if (fillA == fillB) continue;
                if (fillA.overlaps(fillB)) return true;
            }
        }
        return false;
    }

    static Area invert(Area source) {
        if (source instanceof Fill fill) {
            return new FillUnion(out -> FULL.fillsRemove(fill, out));
        } else if (source instanceof FillUnion union) {
            List<Fill> out = new ArrayList<>();
            out.add(FULL);
            for (Fill fill : union.areas) {
                List<Fill> copy = List.copyOf(out);
                out.clear();
                for (Fill existing : copy) {
                    existing.fillsRemove(fill, out::add);
                }
            }
            return AreaImpl.safeUnion(out.toArray(Fill[]::new));
        }
        throw new IllegalArgumentException("Unknown area type for invert: " + source.getClass().getName());
    }

    static Area intersection(Area areaA, Area areaB) {
        return areaA.overlap(areaB);
    }

    record Fill(Vec min, Vec max) implements Area {
        public Fill(Point min, Point max) {
            this(Vec.fromPoint(min), Vec.fromPoint(max));
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

        @Override
        public boolean contains(Area area) {
            return boundsContains(this, area);
        }

        @Override
        public boolean contains(Point point) {
            return contains(point.x(), point.y(), point.z());
        }

        @Override
        public boolean contains(double x, double y, double z) {
            return x >= min.x() && x < max.x() &&
                    y >= min.y() && y < max.y() &&
                    z >= min.z() && z < max.z();
        }

        @Override
        public long size() {
            return (long) (max.blockX() - min.blockX()) * (max.blockY() - min.blockY()) * (max.blockZ() - min.blockZ());
        }

        @Override
        public Area overlap(Area other) {
            if (other instanceof Fill fill) {
                int minX = Math.max(min.blockX(), fill.min.blockX());
                int minY = Math.max(min.blockY(), fill.min.blockY());
                int minZ = Math.max(min.blockZ(), fill.min.blockZ());

                int maxX = Math.min(max.blockX(), fill.max.blockX());
                int maxY = Math.min(max.blockY(), fill.max.blockY());
                int maxZ = Math.min(max.blockZ(), fill.max.blockZ());

                return Area.fill(new Vec(minX, minY, minZ), new Vec(maxX, maxY, maxZ));
            }
            return other.overlap(this); // custom impl
        }

        @Override
        public long overlapCount(Area other) {
            if (other instanceof Fill fill) {
                int minX = Math.max(min.blockX(), fill.min.blockX());
                int minY = Math.max(min.blockY(), fill.min.blockY());
                int minZ = Math.max(min.blockZ(), fill.min.blockZ());

                int maxX = Math.min(max.blockX(), fill.max.blockX());
                int maxY = Math.min(max.blockY(), fill.max.blockY());
                int maxZ = Math.min(max.blockZ(), fill.max.blockZ());

                return (long) (maxX - minX) * (maxY - minY) * (maxZ - minZ);
            }
            return other.overlapCount(this); // custom impl
        }

        @Override
        public boolean overlaps(Area other) {
            if (other instanceof Fill fill) {
                int minX = Math.max(min.blockX(), fill.min.blockX());
                int minY = Math.max(min.blockY(), fill.min.blockY());
                int minZ = Math.max(min.blockZ(), fill.min.blockZ());

                int maxX = Math.min(max.blockX(), fill.max.blockX());
                int maxY = Math.min(max.blockY(), fill.max.blockY());
                int maxZ = Math.min(max.blockZ(), fill.max.blockZ());

                return minX < maxX && minY < maxY && minZ < maxZ;
            }
            return other.overlaps(this); // custom impl
        }

        @Override
        public Area translate(Point point) {
            return new Fill(min.add(point), max.add(point));
        }

        @Override
        public Set<Fill> subdivide() {
            return Set.of(this);
        }

        @Override
        public String toString() {
            return AreaImpl.toString(this);
        }

        private void fillsRemove(Area other, Consumer<Fill> out) {
            // We output the necessary fills to recreate this area without the other area
            // Intersection
            if (other instanceof Fill fill) {
                Vec thisMin = min;
                Vec thisMax = max;
                Vec fillMin = fill.min;
                Vec fillMax = fill.max;

                // No intersection
                if (thisMin.blockX() >= fillMax.blockX() || thisMin.blockY() >= fillMax.blockY() || thisMin.blockZ() >= fillMax.blockZ() ||
                        thisMax.blockX() <= fillMin.blockX() || thisMax.blockY() <= fillMin.blockY() || thisMax.blockZ() <= fillMin.blockZ()) {
                    out.accept(this);
                    return;
                }

                // Remove
                // X > Y > Z

                // shave left
                if (thisMin.blockX() < fillMin.blockX()) {
                    out.accept(fill(thisMin, thisMax.withX(fillMin.blockX())));
                    thisMin = thisMin.withX(fillMin.blockX());
                }

                // shave right
                if (thisMax.blockX() > fillMax.blockX()) {
                    out.accept(fill(thisMin.withX(fillMax.blockX()), thisMax));
                    thisMax = thisMax.withX(fillMax.blockX());
                }

                // shave bottom
                if (thisMin.blockY() < fillMin.blockY()) {
                    out.accept(fill(thisMin, thisMax.withY(fillMin.blockY())));
                    thisMin = thisMin.withY(fillMin.blockY());
                }

                // shave top
                if (thisMax.blockY() > fillMax.blockY()) {
                    out.accept(fill(thisMin.withY(fillMax.blockY()), thisMax));
                    thisMax = thisMax.withY(fillMax.blockY());
                }

                // shave front
                if (thisMin.blockZ() < fillMin.blockZ()) {
                    out.accept(fill(thisMin, thisMax.withZ(fillMin.blockZ())));
                    thisMin = thisMin.withZ(fillMin.blockZ());
                }

                // shave back
                if (thisMax.blockZ() > fillMax.blockZ()) {
                    out.accept(fill(thisMin.withZ(fillMax.blockZ()), thisMax));
                    thisMax = thisMax.withZ(fillMax.blockZ());
                }
                return;
            } else if (other instanceof FillUnion union) {
                for (Fill fill : union.areas) {
                    fillsRemove(fill, out);
                }
                return;
            }
            throw new IllegalArgumentException("Unsupported area type: " + other.getClass().getName());
        }

        private void fillsOverlap(Area other, Consumer<Fill> out) {
            // Intersection
            if (other instanceof Fill fill) {
                int minX = Math.max(min.blockX(), fill.min.blockX());
                int minY = Math.max(min.blockY(), fill.min.blockY());
                int minZ = Math.max(min.blockZ(), fill.min.blockZ());

                int maxX = Math.min(max.blockX(), fill.max.blockX());
                int maxY = Math.min(max.blockY(), fill.max.blockY());
                int maxZ = Math.min(max.blockZ(), fill.max.blockZ());

                if (minX >= maxX || minY >= maxY || minZ >= maxZ) {
                    return;
                }

                // No intersection
                if (minX == min.blockX() && minY == min.blockY() && minZ == min.blockZ() &&
                        maxX == max.blockX() && maxY == max.blockY() && maxZ == max.blockZ()) {
                    out.accept(this);
                    return;
                }

                // Partial intersection
                out.accept(new Fill(min, new Vec(maxX, maxY, maxZ)));
                out.accept(new Fill(new Vec(minX, minY, minZ), max));
                return;
            } else if (other instanceof FillUnion union) {
                for (Fill area : union.areas) {
                    fillsOverlap(area, out);
                }
                return;
            }
            throw new UnsupportedOperationException("Unsupported area type: " + other.getClass().getName());
        }
    }

    record FillUnion(Fill[] areas, Point min, Point max, long size) implements Area {

        public FillUnion(Fill... areas) {
            this(areas, findMin(areas),
                    findMax(areas),
                    findSize(areas));
            for (Fill areaA : areas) {
                for (Fill areaB : areas) {
                    if (areaA == areaB) continue;
                    if (areaA.overlaps(areaB))
                        throw new IllegalArgumentException("Areas overlap: " + areaA + " and " + areaB);
                }
            }
        }

        public FillUnion(Consumer<Consumer<Fill>> fillAccumulator) {
            this(accumulate(fillAccumulator));
        }

        private static Fill[] accumulate(Consumer<Consumer<Fill>> fillAccumulator) {
            Stream.Builder<Fill> builder = Stream.builder();
            fillAccumulator.accept(builder::add);
            return builder.build().toArray(Fill[]::new);
        }

        @Override
        public boolean contains(Area area) {
            return overlapCount(area) == area.size();
        }

        @Override
        public boolean contains(Point point) {
            for (Fill area : areas) {
                if (area.contains(point)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean contains(double x, double y, double z) {
            for (Fill area : areas) {
                if (area.contains(x, y, z)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Area overlap(Area other) {
            List<Fill> out = new ArrayList<>();
            for (Fill area : areas) {
                area.fillsOverlap(other, out::add);
            }
            return AreaImpl.safeUnion(out.toArray(Fill[]::new));
        }

        @Override
        public long overlapCount(Area other) {
            long count = 0;
            for (Fill area : areas) {
                count += area.overlapCount(other);
            }
            return count;
        }

        @Override
        public boolean overlaps(Area other) {
            for (Fill area : areas) {
                if (area.overlaps(other)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Area translate(Point point) {
            Area[] newAreas = new Fill[areas.length];
            for (int i = 0; i < areas.length; i++) {
                newAreas[i] = areas[i].translate(point);
            }
            return Area.union(newAreas);
        }

        @Override
        public Set<Fill> subdivide() {
            return Set.of(areas);
        }

        @NotNull
        @Override
        public Iterator<Point> iterator() {
            return Stream.of(areas).flatMap(a -> StreamSupport.stream(a.spliterator(), false)).iterator();
        }

        @Override
        public String toString() {
            return AreaImpl.toString(this);
        }
    }

    // Analysis methods

    static Point findMin(Point... points) {
        double minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (Point point : points) {
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            minZ = Math.min(minZ, point.z());
        }
        return new Vec(minX, minY, minZ);
    }

    static Point findMin(Area... areas) {
        double minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (Area area : areas) {
            minX = Math.min(minX, area.min().x());
            minY = Math.min(minY, area.min().y());
            minZ = Math.min(minZ, area.min().z());
        }
        return new Vec(minX, minY, minZ);
    }

    static Point findMax(Point... points) {
        double maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Point point : points) {
            maxX = Math.max(maxX, point.x());
            maxY = Math.max(maxY, point.y());
            maxZ = Math.max(maxZ, point.z());
        }
        return new Vec(maxX, maxY, maxZ);
    }

    static Point findMax(Area... areas) {
        double maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Area area : areas) {
            maxX = Math.max(maxX, area.max().x());
            maxY = Math.max(maxY, area.max().y());
            maxZ = Math.max(maxZ, area.max().z());
        }
        return new Vec(maxX, maxY, maxZ);
    }

    // Note that these areas are non-intersecting
    static long findSize(Area[] areas) {
        return Stream.of(areas).mapToLong(Area::size).sum();
    }

    static boolean boundsContains(Area area, Area other) {
        Point min = area.min();
        Point max = area.max();

        Point otherMin = other.min();
        Point otherMax = other.max();

        return min.blockX() <= otherMin.blockX() &&
                min.blockY() <= otherMin.blockY() &&
                min.blockZ() <= otherMin.blockZ() &&
                max.blockX() >= otherMax.blockX() &&
                max.blockY() >= otherMax.blockY() &&
                max.blockZ() >= otherMax.blockZ();
    }

    static String toString(Area area) {
        return toString(area, 0);
    }

    static String toString(Area area, int depth) {
        String prefix = String.join("", Collections.nCopies(depth, "  "));
        if (area instanceof Fill fill) {
            return prefix + "Fill(" + fill.min + ", " + fill.max + ")";
        }
        if (area instanceof FillUnion union) {
            String contents = Stream.of(union.areas)
                    .map(a -> toString(a, depth + 1))
                    .collect(Collectors.joining(",\n"));

            return prefix + "Union(\n" + contents + "\n" + prefix + ")";
        }
        return prefix + area.toString();
    }
}