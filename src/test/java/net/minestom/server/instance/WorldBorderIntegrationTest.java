package net.minestom.server.instance;

import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnvTest
public class WorldBorderIntegrationTest {

    @Test
    public void setWorldborderSize(Env env) {
        Instance instance = env.createFlatInstance();

        instance.worldBorder().setDiameter(50.0);
        assertEquals(50.0, instance.worldBorder().getDiameter());
        instance.worldBorder().setDiameter(10.0);
        assertEquals(10.0, instance.worldBorder().getDiameter());
    }

    @Test
    public void resizeWorldBorder(Env env) throws InterruptedException {
        Instance instance = env.createFlatInstance();

        instance.worldBorder().setDiameter(50.0);

        instance.worldBorder().setDiameter(10.0, 1);
        assertEquals(50.0, instance.worldBorder().getDiameter());

        Thread.sleep(10);
        instance.worldBorder().update();
        assertEquals(10.0, instance.worldBorder().getDiameter());
    }
}
