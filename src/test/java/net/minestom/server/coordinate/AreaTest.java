package net.minestom.server.coordinate;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

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
    public void complexUnionArea() {
        Area areaA = Area.fill(new Vec(0), new Vec(100));
        Area areaB = Area.fill(new Vec(50), new Vec(100));
        Area areaC = Area.fill(new Vec(50), new Vec(51));
        Area area = Area.union(areaA, areaB, areaC);

        assertContains(area, 0, 0, 0);
        assertEquals(100 * 100 * 100, area.size());
        assertEquals(2 * 2 * 2, area.overlapCount(Area.fill(new Vec(50), new Vec(52))));
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
