package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;

import static net.minestom.testing.TestUtils.waitUntilCleared;

@EnvTest
public class InstanceUnregisterIntegrationTest {

    @Test
    public void instanceGC(Env env) {
        var instance = env.createFlatInstance();
        var ref = new WeakReference<>(instance);
        env.process().instance().unregisterInstance(instance);

        //noinspection UnusedAssignment
        instance = null;
        waitUntilCleared(ref);
    }

    @Test
    public void instanceNodeGC(Env env) {
        final class Game {
            final Instance instance;

            Game(Env env) {
                instance = env.process().instance().createInstanceContainer();
                instance.eventNode().addListener(PlayerMoveEvent.class, e -> System.out.println(instance));
            }
        }
        var game = new Game(env);
        var ref = new WeakReference<>(game);
        env.process().instance().unregisterInstance(game.instance);

        //noinspection UnusedAssignment
        game = null;
        waitUntilCleared(ref);
    }

    @Test
    public void chunkGC(Env env) {
        // Ensure that unregistering an instance does release its chunks
        var instance = env.createFlatInstance();
        var chunk = instance.loadArea(Area.chunk(instance.dimensionType(), 0, 0)).join();
        var ref = new WeakReference<>(chunk);
        instance.unloadArea(chunk.area()).join();
        env.process().instance().unregisterInstance(instance);
        env.tick(); // Required to remove the chunk from the thread dispatcher

        //noinspection UnusedAssignment
        chunk = null;
        waitUntilCleared(ref);
    }
}
