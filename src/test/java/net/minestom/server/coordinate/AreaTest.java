package net.minestom.server.coordinate;

import net.minestom.server.instance.Instance;
import net.minestom.server.world.DimensionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AreaTest {

    @Test
    public void singleFillArea() {
        Area area = Area.fill(new Vec(0, 0, 0), new Vec(1, 1, 1));

        assertContains(area, 0, 0, 0);
        assertNotContains(area, 0, 0, 1);
    }

    @Test
    public void smallFillArea() {
        Area area = Area.fill(new Vec(0, 0, 0), new Vec(2, 2, 2));

        assertContains(area, 0, 0, 0);
        assertContains(area, 1, 1, 1);
        assertNotContains(area, 0, 0, 3);
        assertNotContains(area, 3, 0, 0);
        assertNotContains(area, 0, 3, 0);
    }

    @Test
    public void largeFillArea() {
        Area area = Area.fill(new Vec(0, 0, 0), new Vec(100, 100, 100));

        assertContains(area, 0, 0, 0);
        assertContains(area, 50, 50, 50);
        assertContains(area, 99, 99, 99);
        assertNotContains(area, 0, 0, 101);
    }

    @Test
    public void singleCollectionArea() {
        Area area = Area.collection(List.of(new Vec(0, 0, 0)));

        assertContains(area, 0, 0, 0);
        assertNotContains(area, 0, 0, 1);
    }

    @Test
    public void smallCollectionArea() {
        Area area = Area.collection(new Vec(0, 0, 0), new Vec(1, 0, 0));

        assertContains(area, 0, 0, 0);
        assertContains(area, 1, 0, 0);
        assertNotContains(area, 0, 0, 1);
        assertNotContains(area, 2, 0, 0);
    }

    @Test
    public void unionFillCollection() {
        Area area = Area.union(
            Area.fill(new Vec(0, 0, 0), new Vec(2, 2, 2)),
            Area.collection(List.of(new Vec(2, 1, 1)))
        );

        assertContains(area, 0, 0, 0);
        assertContains(area, 1, 1, 1);
        assertContains(area, 2, 1, 1);
        assertNotContains(area, 0, 0, 3);
        assertNotContains(area, 2, 0, 0);
        assertNotContains(area, 3, 0, 0);
    }

    @Test
    public void intersectionFillCollection() {
        Area area = Area.fill(new Vec(0, 0, 0), new Vec(2, 2, 2))
                .overlap(Area.collection(List.of(new Vec(1, 1, 1))));

        assertNotContains(area, 0, 0, 0);
        assertContains(area, 1, 1, 1);
        assertNotContains(area, 0, 0, 1);
        assertNotContains(area, 1, 0, 0);
        assertNotContains(area, 0, 1, 0);
        assertNotContains(area, 0, 0, 2);
    }

    @Test
    public void invertInvertFillArea() {
        Area inverted = Area.invert(Area.fill(new Vec(0, 0, 0), new Vec(1, 1, 1)));
        Area area = Area.invert(inverted);

        assertContains(area, 0, 0, 0);
        assertNotContains(area, 0, 0, 1);
        assertNotContains(area, 0, 1, 0);
        assertNotContains(area, 0, 1, 1);
        assertNotContains(area, 1, 0, 0);

        // Exotic cases
        assertNotContains(area, 98, 87, 76);
        assertNotContains(area, -98, -87, -76);
    }

    @Test
    public void invertFillArea() {
        Area area = Area.invert(Area.fill(new Vec(0), new Vec(1)));

        assertNotContains(area, 0, 0, 0);

        // Test every axis + every combination of axes
        // x,y,z
        assertContains(area, 1, 0, 0);
        assertContains(area, -1, 0, 0);
        assertContains(area, 0, 1, 0);
        assertContains(area, 0, -1, 0);
        assertContains(area, 0, 0, 1);
        assertContains(area, 0, 0, -1);

        // xy
        assertContains(area, 1, 1, 0);
        assertContains(area, 1, -1, 0);
        assertContains(area, -1, 1, 0);
        assertContains(area, -1, -1, 0);

        // xz
        assertContains(area, 1, 0, 1);
        assertContains(area, 1, 0, -1);
        assertContains(area, -1, 0, 1);
        assertContains(area, -1, 0, -1);

        // yz
        assertContains(area, 0, 1, 1);
        assertContains(area, 0, 1, -1);
        assertContains(area, 0, -1, 1);
        assertContains(area, 0, -1, -1);

        // xyz
        assertContains(area, 1, 1, 1);
        assertContains(area, 1, 1, -1);
        assertContains(area, 1, -1, 1);
        assertContains(area, 1, -1, -1);
        assertContains(area, -1, 1, 1);
        assertContains(area, -1, 1, -1);
        assertContains(area, -1, -1, 1);
        assertContains(area, -1, -1, -1);

    }

    @Test
    public void invertUnionArea() {
        Area area = Area.invert(Area.union(
                Area.fill(new Vec(0, 0, 0), new Vec(1, 1, 1)),
                Area.fill(new Vec(2, 2, 2), new Vec(3, 3, 3))
        ));

        assertNotContains(area, 0, 0, 0);
        assertNotContains(area, 2, 2, 2);
        assertContains(area, 0, 0, 1);
        assertContains(area, 0, 1, 0);
        assertContains(area, 0, 1, 1);
        assertContains(area, 1, 0, 0);
        assertContains(area, 1, 1, 1);
        assertContains(area, 2, 2, 3);
        assertContains(area, 3, 3, 3);
        assertContains(area, 2, 3, 3);
        assertContains(area, 3, 2, 3);
        assertContains(area, 3, 3, 2);
        assertContains(area, 2, 3, 2);
        assertContains(area, 3, 2, 2);
    }

    @Test
    public void overlapArea() {
        Area larger = Area.union(
                Area.fill(new Vec(-304, -64, -304), new Vec(0, 320, 304)),
                Area.fill(new Vec(0, -64, -304), new Vec(304, 320, 304))
        );
        Area smaller = Area.union(
                Area.fill(new Vec(-128, -64, -128), new Vec(0, 320, 128)),
                Area.fill(new Vec(0, -64, -128), new Vec(128, 320, 128))
        );

        Area overlappedA = larger.overlap(smaller);

        assertEquals(overlappedA.size(), smaller.size());
    }

    @Test
    public void complexUnionArea() {
        Area areaA = Area.fill(new Vec(0), new Vec(100));
        Area areaB = Area.fill(new Vec(50), new Vec(100));
        Area areaC = Area.fill(new Vec(50), new Vec(51));
        Area area = Area.union(areaA, areaB, areaC);

        assertContains(area, 0, 0, 0);
        assertEquals(100 * 100 * 100, area.size());
        assertEquals(2 * 2 * 2, area.overlapCount(Area.fill(new Vec(50), new Vec(52))));
    }

    @Test
    public void excludeEmpty() {
        Area area = Area.exclude(Area.fill(new Vec(-100), new Vec(100)), Area.empty());

        assertContains(area, 0, 0, 0);
        assertEquals(200 * 200 * 200, area.size());
    }

    @Test
    public void chunkRadiusTest() {
        int radius = 8;
        Area area = Area.chunkRange(DimensionType.OVERWORLD, 0, 0, radius);
        int side = radius * 2 + 1; // +1 for the center chunks
        int sideBlocks = side * Instance.SECTION_SIZE;
        int yBlocks = area.max().blockY() - area.min().blockY();

        int minX = -radius * Instance.SECTION_SIZE;
        int maxX = (radius + 1) * Instance.SECTION_SIZE;
        int minZ = -radius * Instance.SECTION_SIZE;
        int maxZ = (radius + 1) * Instance.SECTION_SIZE;

        assertEquals(minX, area.min().blockX());
        assertEquals(maxX, area.max().blockX());
        assertEquals(minZ, area.min().blockZ());
        assertEquals(maxZ, area.max().blockZ());

        assertEquals((long) sideBlocks * yBlocks * sideBlocks, area.size());
    }

    private void assertContains(Area area, double x, double y, double z) {
        assertContains(true, area, new Vec(x, y, z));
    }

    private void assertNotContains(Area area, double x, double y, double z) {
        assertContains(false, area, new Vec(x, y, z));
    }

    private void assertContains(boolean contains, Area area, Vec vec) {
        assertEquals(contains, area.contains(vec), () -> "Point(" + vec.x() + ", " + vec.y() + ", " + vec.z() + ") should " + (contains ? "" : "not ") + "be in the area.");
    }
}
