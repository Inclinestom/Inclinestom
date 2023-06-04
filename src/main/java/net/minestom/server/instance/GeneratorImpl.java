package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.UnitModifier;
import net.minestom.server.instance.storage.WorldView;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static net.minestom.server.utils.chunk.ChunkUtils.*;

final class GeneratorImpl {
    private static final int SIZE = Instance.SECTION_SIZE;
    private static final int BIOME_SIZE = Instance.BIOME_SIZE;
    private static final Vec SECTION_SIZE = new Vec(SIZE);

    static GenerationUnit section(WorldView.Mutable section, int sectionX, int sectionY, int sectionZ, boolean fork) {
        final Vec start = SECTION_SIZE.mul(sectionX, sectionY, sectionZ);
        final Vec end = start.add(SECTION_SIZE);
        final UnitModifier modifier = new SectionModifierImpl(SECTION_SIZE, start, end, section, fork);
        return unit(modifier, start, end, null);
    }

    static GenerationUnit section(int sectionX, int sectionY, int sectionZ, boolean fork) {
        Point pos = new Vec(sectionX, sectionY, sectionZ).mul(SIZE);
        WorldView.Mutable relative = WorldView.mutable(Area.section(Vec.ZERO));
        return section(WorldView.translate(relative, pos), sectionX, sectionY, sectionZ, fork);
    }

    static List<UnitImpl> mutable(Area area) {
        return mutable(WorldView.mutable(area));
    }

    static List<UnitImpl> mutable(WorldView.Mutable memory) {
        Area area = memory.area();
        Point start = area.min();
        Point end = area.max();
        Point size = end.sub(start);
        List<UnitImpl> units = new ArrayList<>();
        for (Area sub : area.subdivide()) {
            WorldView.Mutable subMemory = WorldView.view(memory, sub);
            UnitModifier modifier = new CubeWorldViewModifierImpl(subMemory, sub.min(), sub.max());
            units.add(unit(modifier, sub.min(), sub.max(), null));
        }
        return List.copyOf(units);
    }

    static UnitImpl unit(UnitModifier modifier, Point start, Point end,
                         List<GenerationUnit> divided) {
        if (start.x() > end.x() || start.y() > end.y() || start.z() > end.z()) {
            throw new IllegalArgumentException("absoluteStart must be before absoluteEnd");
        }
        final Point size = end.sub(start);
        return new UnitImpl(modifier, size, start, end, divided, new CopyOnWriteArrayList<>());
    }
    static final class DynamicFork implements Block.Setter {
        Vec minSection;
        int width, height, depth;
        List<GenerationUnit> sections;

        @Override
        public void setBlock(int x, int y, int z, Block block) {
            resize(x, y, z);
            GenerationUnit section = findAbsolute(sections, minSection, width, height, depth, x, y, z);
            assert section.absoluteStart().sectionX() == getSectionCoordinate(x) &&
                    section.absoluteStart().sectionY() == getSectionCoordinate(y) &&
                    section.absoluteStart().sectionZ() == getSectionCoordinate(z) :
                    "Invalid section " + section.absoluteStart() + " for " + x + ", " + y + ", " + z;
            section.modifier().setBlock(x, y, z, block);
        }

        private void resize(int x, int y, int z) {
            final int sectionX = getSectionCoordinate(x);
            final int sectionY = getSectionCoordinate(y);
            final int sectionZ = getSectionCoordinate(z);
            if (sections == null) {
                this.minSection = new Vec(sectionX, sectionY, sectionZ).mul(SECTION_SIZE);
                this.width = 1;
                this.height = 1;
                this.depth = 1;
                this.sections = List.of(section(sectionX, sectionY, sectionZ, true));
            } else if (x < minSection.x() || y < minSection.y() || z < minSection.z() ||
                    x >= minSection.x() + width * SIZE || y >= minSection.y() + height * SIZE || z >= minSection.z() + depth * SIZE) {

                // Resize is necessary
                final Vec newMin = new Vec(Math.min(minSection.x(), sectionX * SIZE),
                        Math.min(minSection.y(), sectionY * SIZE),
                        Math.min(minSection.z(), sectionZ * SIZE));
                final Vec newMax = new Vec(Math.max(minSection.x() + width * SIZE, sectionX * SIZE + SIZE),
                        Math.max(minSection.y() + height * SIZE, sectionY * SIZE + SIZE),
                        Math.max(minSection.z() + depth * SIZE, sectionZ * SIZE + SIZE));
                final int newWidth = getSectionCoordinate(newMax.x() - newMin.x());
                final int newHeight = getSectionCoordinate(newMax.y() - newMin.y());
                final int newDepth = getSectionCoordinate(newMax.z() - newMin.z());

                // Resize
                GenerationUnit[] newSections = new GenerationUnit[newWidth * newHeight * newDepth];

                // Copy old sections
                for (GenerationUnit s : sections) {
                    final Point start = s.absoluteStart();
                    final int newX = getSectionCoordinate(start.x() - newMin.x());
                    final int newY = getSectionCoordinate(start.y() - newMin.y());
                    final int newZ = getSectionCoordinate(start.z() - newMin.z());
                    final int index = findIndex(newWidth, newHeight, newDepth, newX, newY, newZ);
                    newSections[index] = s;
                }

                // Fill new sections
                final int startX = newMin.sectionX();
                final int startY = newMin.sectionY();
                final int startZ = newMin.sectionZ();
                for (int i = 0; i < newSections.length; i++) {
                    if (newSections[i] == null) {
                        final Point coordinates = to3D(i, newWidth, newHeight, newDepth);
                        final int newX = coordinates.blockX() + startX;
                        final int newY = coordinates.blockY() + startY;
                        final int newZ = coordinates.blockZ() + startZ;
                        final GenerationUnit unit = section(newX, newY, newZ, true);
                        newSections[i] = unit;
                    }
                }

                this.sections = List.of(newSections);
                this.minSection = newMin;
                this.width = newWidth;
                this.height = newHeight;
                this.depth = newDepth;
            }
        }
    }

    record UnitImpl(UnitModifier modifier, Point size,
                    Point absoluteStart, Point absoluteEnd,
                    @Nullable List<GenerationUnit> divided,
                    List<UnitImpl> forks) implements GenerationUnit {
        @Override
        public GenerationUnit fork(Point start, Point end) {
            final int minSectionX = floorSection(start.blockX()) / SIZE;
            final int minSectionY = floorSection(start.blockY()) / SIZE;
            final int minSectionZ = floorSection(start.blockZ()) / SIZE;

            final int maxSectionX = ceilSection(end.blockX()) / SIZE;
            final int maxSectionY = ceilSection(end.blockY()) / SIZE;
            final int maxSectionZ = ceilSection(end.blockZ()) / SIZE;

            final int width = maxSectionX - minSectionX;
            final int height = maxSectionY - minSectionY;
            final int depth = maxSectionZ - minSectionZ;

            GenerationUnit[] units = new GenerationUnit[width * height * depth];
            int index = 0;
            for (int sectionX = minSectionX; sectionX < maxSectionX; sectionX++) {
                for (int sectionY = minSectionY; sectionY < maxSectionY; sectionY++) {
                    for (int sectionZ = minSectionZ; sectionZ < maxSectionZ; sectionZ++) {
                        final GenerationUnit unit = section(sectionX, sectionY, sectionZ, true);
                        units[index++] = unit;
                    }
                }
            }
            final List<GenerationUnit> sections = List.of(units);
            final Point startSection = new Vec(minSectionX * SIZE, minSectionY * SIZE, minSectionZ * SIZE);
            return registerFork(startSection, sections, width, height, depth);
        }

        @Override
        public void fork(Consumer<Block.Setter> consumer) {
            DynamicFork dynamicFork = new DynamicFork();
            consumer.accept(dynamicFork);
            final Point startSection = dynamicFork.minSection;
            if (startSection == null)
                return; // No block has been placed
            final int width = dynamicFork.width;
            final int height = dynamicFork.height;
            final int depth = dynamicFork.depth;
            final List<GenerationUnit> sections = dynamicFork.sections;
            registerFork(startSection, sections, width, height, depth);
        }

        @Override
        public List<GenerationUnit> subdivide() {
            return Objects.requireNonNullElseGet(divided, GenerationUnit.super::subdivide);
        }

        private GenerationUnit registerFork(Point start, List<GenerationUnit> sections,
                                            int width, int height, int depth) {
            final Point end = start.add(width * SIZE, height * SIZE, depth * SIZE);
            final Point size = end.sub(start);
            final BoxedModifierImpl modifier = new BoxedModifierImpl(size, start, end, width, height, depth, sections);
            final UnitImpl fork = new UnitImpl(modifier, size, start, end, sections, forks);
            forks.add(fork);
            return fork;
        }
    }

    record SectionModifierImpl(Point size, Point start, Point end,
                               WorldView.Mutable section, boolean fork) implements GenericModifier {
        @Override
        public void setBiome(int x, int y, int z, Biome biome) {
            if (fork) throw new IllegalStateException("Cannot modify biomes of a fork");

            section.setBiome(
                    toSectionRelativeCoordinate(x) / BIOME_SIZE,
                    toSectionRelativeCoordinate(y) / BIOME_SIZE,
                    toSectionRelativeCoordinate(z) / BIOME_SIZE, biome);
        }

        @Override
        public void setBlock(int x, int y, int z, Block block) {
            final int localX = toSectionRelativeCoordinate(x);
            final int localY = toSectionRelativeCoordinate(y);
            final int localZ = toSectionRelativeCoordinate(z);
            this.section.setBlock(localX, localY, localZ, block);
        }

        @Override
        public void setRelative(int x, int y, int z, Block block) {
            this.section.setBlock(x, y, z, block);
        }

        @Override
        public void setAllRelative(Supplier supplier) {
            for (int x = 0; x < Instance.SECTION_SIZE; x++) {
                for (int y = 0; y < Instance.SECTION_SIZE; y++) {
                    for (int z = 0; z < Instance.SECTION_SIZE; z++) {
                        final Block block = supplier.get(x, y, z);
                        this.section.setBlock(x, y, z, block);
                    }
                }
            }
        }

        @Override
        public void fill(Block block) {
            setAllRelative((x, y, z) -> block);
        }

        @Override
        public void fillBiome(Biome biome) {
            if (fork) throw new IllegalStateException("Cannot modify biomes of a fork");
            for (int x = 0; x < Instance.SECTION_SIZE; x++) {
                for (int y = 0; y < Instance.SECTION_SIZE; y++) {
                    for (int z = 0; z < Instance.SECTION_SIZE; z++) {
                        section.setBiome(x / BIOME_SIZE, y / BIOME_SIZE, z / BIOME_SIZE, biome);
                    }
                }
            }
        }
    }

    record CubeWorldViewModifierImpl(WorldView.Mutable worldView, Point start, Point end) implements GenericModifier {

        @Override
        public void setBlock(int x, int y, int z, Block block) {
            worldView.setBlock(x, y, z, block);
        }

        @Override
        public void setRelative(int x, int y, int z, Block block) {
            worldView.setBlock(x + start.blockX(), y + start.blockY(), z + start.blockZ(), block);
        }

        @Override
        public void fill(Block block) {
            // TODO: Optimize this through area operations
            fill(start, end, block);
        }

        @Override
        public void fill(Point start, Point end, Block block) {
            int startX = Math.max(start.blockX(), this.start.blockX());
            int startY = Math.max(start.blockY(), this.start.blockY());
            int startZ = Math.max(start.blockZ(), this.start.blockZ());

            int endX = Math.min(end.blockX(), this.end.blockX());
            int endY = Math.min(end.blockY(), this.end.blockY());
            int endZ = Math.min(end.blockZ(), this.end.blockZ());

            worldView.mutate(setter -> {
                for (int x = startX; x < endX; x++) {
                    for (int y = startY; y < endY; y++) {
                        for (int z = startZ; z < endZ; z++) {
                            setter.setBlock(x, y, z, block);
                        }
                    }
                }
            });
        }

        @Override
        public void fillBiome(Biome biome) {
            for (int x = start.blockX(); x < end.blockX(); x += Instance.BIOME_SIZE) {
                for (int y = start.blockY(); y < end.blockY(); y += Instance.BIOME_SIZE) {
                    for (int z = start.blockZ(); z < end.blockZ(); z += Instance.BIOME_SIZE) {
                        worldView.setBiome(x, y, z, biome);
                    }
                }
            }
        }

        @Override
        public void fillHeight(int minHeight, int maxHeight, Block block) {
            final Point start = start();
            final Point end = end();
            final int startY = start.blockY();
            final int endY = end.blockY();
            Point min = start.withY(Math.max(minHeight, startY));
            Point max = end.withY(Math.min(maxHeight, endY));
            Area fillArea = Area.fill(min, max);
            for (Area fillBox : worldView().area().overlap(fillArea).subdivide()) {
                fill(fillBox.min(), fillBox.max(), block);
            }
        }

        @Override
        public void setBiome(int x, int y, int z, Biome biome) {
            worldView.setBiome(x, y, z, biome);
        }
    }

    record BoxedModifierImpl(
            Point size, Point start, Point end,
                             int width, int height, int depth,
                             List<GenerationUnit> sections) implements GenericModifier {
        @Override
        public void setBlock(int x, int y, int z, Block block) {
            checkBorder(x, y, z);
            final GenerationUnit section = findAbsoluteSection(x, y, z);
            y -= start.y();
            section.modifier().setBlock(x, y, z, block);
        }

        @Override
        public void setBiome(int x, int y, int z, Biome biome) {
            checkBorder(x, y, z);
            final GenerationUnit section = findAbsoluteSection(x, y, z);
            y -= start.y();
            section.modifier().setBiome(x, y, z, biome);
        }

        @Override
        public void setRelative(int x, int y, int z, Block block) {
            if (x < 0 || x >= size.x() || y < 0 || y >= size.y() || z < 0 || z >= size.z()) {
                throw new IllegalArgumentException("x, y and z must be in the chunk: " + x + ", " + y + ", " + z);
            }
            final GenerationUnit section = findRelativeSection(x, y, z);
            x = toSectionRelativeCoordinate(x);
            y = toSectionRelativeCoordinate(y);
            z = toSectionRelativeCoordinate(z);
            section.modifier().setBlock(x, y, z, block);
        }

        @Override
        public void setAll(Supplier supplier) {
            for (GenerationUnit section : sections) {
                final var start = section.absoluteStart();
                final int startX = start.blockX();
                final int startY = start.blockY();
                final int startZ = start.blockZ();
                section.modifier().setAllRelative((x, y, z) ->
                        supplier.get(x + startX, y + startY, z + startZ));
            }
        }

        @Override
        public void setAllRelative(Supplier supplier) {
            final Point start = this.start;
            for (GenerationUnit section : sections) {
                final Point sectionStart = section.absoluteStart();
                final int offsetX = sectionStart.blockX() - start.blockX();
                final int offsetY = sectionStart.blockY() - start.blockY();
                final int offsetZ = sectionStart.blockZ() - start.blockZ();
                section.modifier().setAllRelative((x, y, z) ->
                        supplier.get(x + offsetX, y + offsetY, z + offsetZ));
            }
        }

        @Override
        public void fill(Block block) {
            for (GenerationUnit section : sections) {
                section.modifier().fill(block);
            }
        }

        @Override
        public void fillBiome(Biome biome) {
            for (GenerationUnit section : sections) {
                section.modifier().fillBiome(biome);
            }
        }

        @Override
        public void fillHeight(int minHeight, int maxHeight, Block block) {
            final Point start = this.start;
            final int width = this.width;
            final int depth = this.depth;
            final int startX = start.blockX();
            final int startZ = start.blockZ();
            final int minMultiple = floorSection(minHeight);
            final int maxMultiple = ceilSection(maxHeight);
            final boolean startOffset = minMultiple != minHeight;
            final boolean endOffset = maxMultiple != maxHeight;
            if (startOffset || endOffset) {
                final int firstFill = Math.min(minMultiple + SIZE, maxHeight);
                final int lastFill = startOffset ? Math.max(firstFill, floorSection(maxHeight)) : floorSection(maxHeight);
                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < depth; z++) {
                        final int sectionX = startX + x * SIZE;
                        final int sectionZ = startZ + z * SIZE;
                        // Fill start
                        if (startOffset) {
                            final GenerationUnit section = findAbsoluteSection(sectionX, minMultiple, sectionZ);
                            section.modifier().fillHeight(minHeight, firstFill, block);
                        }
                        // Fill end
                        if (endOffset) {
                            final GenerationUnit section = findAbsoluteSection(sectionX, maxHeight, sectionZ);
                            section.modifier().fillHeight(lastFill, maxHeight, block);
                        }
                    }
                }
            }
            // Middle sections (to fill)
            final int startSection = (minMultiple) / SIZE + (startOffset ? 1 : 0);
            final int endSection = (maxMultiple) / SIZE + (endOffset ? -1 : 0);
            for (int i = startSection; i < endSection; i++) {
                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < depth; z++) {
                        final GenerationUnit section = findAbsoluteSection(startX + x * SIZE, i * SIZE, startZ + z * SIZE);
                        section.modifier().fill(block);
                    }
                }
            }
        }

        private GenerationUnit findAbsoluteSection(int x, int y, int z) {
            return findAbsolute(sections, start, width, height, depth, x, y, z);
        }

        private GenerationUnit findRelativeSection(int x, int y, int z) {
            final int sectionX = getSectionCoordinate(x);
            final int sectionY = getSectionCoordinate(y);
            final int sectionZ = getSectionCoordinate(z);
            final int index = sectionZ + sectionY * depth + sectionX * depth * height;
            return sections.get(index);
        }

        private void checkBorder(int x, int y, int z) {
            if (x < start.x() || x >= end.x() ||
                    y < start.y() || y >= end.y() ||
                    z < start.z() || z >= end.z()) {
                final String format = String.format("Invalid coordinates: %d, %d, %d for worldView %s %s", x, y, z, start, end);
                throw new IllegalArgumentException(format);
            }
        }
    }

    sealed interface GenericModifier extends UnitModifier
            permits CubeWorldViewModifierImpl, BoxedModifierImpl, SectionModifierImpl {

        Point start();

        Point end();

        @Override
        default void setAll(Supplier supplier) {
            final Point start = start();
            final Point end = end();
            final int endX = end.blockX();
            final int endY = end.blockY();
            final int endZ = end.blockZ();
            for (int x = start.blockX(); x < endX; x++) {
                for (int y = start.blockY(); y < endY; y++) {
                    for (int z = start.blockZ(); z < endZ; z++) {
                        setBlock(x, y, z, supplier.get(x, y, z));
                    }
                }
            }
        }

        @Override
        default void setAllRelative(Supplier supplier) {
            final Point start = start();
            final Point end = end();
            final Point size = end.sub(start);

            for (int x = 0; x < size.blockX(); x++) {
                for (int y = 0; y < size.blockY(); y++) {
                    for (int z = 0; z < size.blockZ(); z++) {
                        setRelative(x, y, z, supplier.get(x, y, z));
                    }
                }
            }
        }

        @Override
        default void fill(Block block) {
            fill(start(), end(), block);
        }

        @Override
        default void fill(Point start, Point end, Block block) {
            final int endX = end.blockX();
            final int endY = end.blockY();
            final int endZ = end.blockZ();
            for (int x = start.blockX(); x < endX; x++) {
                for (int y = start.blockY(); y < endY; y++) {
                    for (int z = start.blockZ(); z < endZ; z++) {
                        setBlock(x, y, z, block);
                    }
                }
            }
        }

        @Override
        default void fillHeight(int minHeight, int maxHeight, Block block) {
            final Point start = start();
            final Point end = end();
            final int startY = start.blockY();
            final int endY = end.blockY();
            if (startY >= minHeight && endY <= maxHeight) {
                // Fast path if the unit is fully contained in the height range
                // TODO: FillAll optimization
                fill(start, end, block);
            } else {
                // Slow path if the unit is not fully contained in the height range
                Point min = start.withY(Math.max(minHeight, startY));
                Point max = end.withY(Math.min(maxHeight, endY));
                fill(min, max, block);
            }
        }
    }

    private static GenerationUnit findAbsolute(List<GenerationUnit> units, Point start,
                                               int width, int height, int depth,
                                               int x, int y, int z) {
        final int sectionX = getSectionCoordinate(x - start.x());
        final int sectionY = getSectionCoordinate(y - start.y());
        final int sectionZ = getSectionCoordinate(z - start.z());
        final int index = findIndex(width, height, depth, sectionX, sectionY, sectionZ);
        return units.get(index);
    }

    private static int findIndex(int width, int height, int depth,
                                 int x, int y, int z) {
        return (z * width * height) + (y * width) + x;
    }

    private static Point to3D(int idx, int width, int height, int depth) {
        final int z = idx / (width * height);
        idx -= (z * width * height);
        final int y = idx / width;
        final int x = idx % width;
        return new Vec(x, y, z);
    }
}
