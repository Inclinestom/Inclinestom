package net.minestom.server.coordinate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PointTest {

    @Test
    public void testNegativeSection() {
        Point vec = new Vec(-1, -1, -1);
        Point pos = new Pos(-1, -1, -1);

        Assertions.assertEquals(vec.sectionX(), -1);
        Assertions.assertEquals(vec.sectionY(), -1);
        Assertions.assertEquals(vec.sectionZ(), -1);
        Assertions.assertEquals(pos.sectionX(), -1);
        Assertions.assertEquals(pos.sectionY(), -1);
        Assertions.assertEquals(pos.sectionZ(), -1);
    }

}
