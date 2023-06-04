package net.minestom.server.entity.player;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.storage.WorldView;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import net.minestom.testing.TestConnection;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionPacket;
import net.minestom.server.network.packet.client.play.ClientTeleportConfirmPacket;
import net.minestom.server.network.packet.server.play.ChunkDataPacket;
import net.minestom.server.network.packet.server.play.EntityPositionPacket;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnvTest
public class PlayerMovementIntegrationTest {

    @Test
    public void teleportConfirm(Env env) {
        var instance = env.createFlatInstance();
        var p1 = env.createPlayer(instance, new Pos(0, 40, 0));
        // No confirmation
        p1.addPacketToQueue(new ClientPlayerPositionPacket(new Pos(0.2, 40, 0), true));
        p1.interpretPacketQueue();
        assertEquals(new Pos(0, 40, 0), p1.getPosition());
        // Confirmation
        p1.addPacketToQueue(new ClientTeleportConfirmPacket(p1.getLastSentTeleportId()));
        p1.addPacketToQueue(new ClientPlayerPositionPacket(new Pos(0.2, 40, 0), true));
        p1.interpretPacketQueue();
        assertEquals(new Pos(0.2, 40, 0), p1.getPosition());
    }

    // FIXME
    //@Test
    public void singleTickMovementUpdate(Env env) {
        var instance = env.createFlatInstance();
        var connection = env.createConnection();
        var p1 = env.createPlayer(instance, new Pos(0, 40, 0));
        connection.connect(instance, new Pos(0, 40, 0)).join();

        p1.addPacketToQueue(new ClientTeleportConfirmPacket(p1.getLastSentTeleportId()));
        p1.addPacketToQueue(new ClientPlayerPositionPacket(new Pos(0.2, 40, 0), true));
        p1.addPacketToQueue(new ClientPlayerPositionPacket(new Pos(0.4, 40, 0), true));
        var tracker = connection.trackIncoming(EntityPositionPacket.class);
        p1.interpretPacketQueue();

        // Position update should only be sent once per tick independently of the number of packets
        tracker.assertSingle();
    }

    @Test
    public void chunkUpdateDebounceTest(Env env) {
        final Instance flatInstance = env.createFlatInstance();
        final int viewDiameter = MinecraftServer.getChunkViewDistance() * 2 + 1;

        // Preload all possible chunks to avoid issues due to async loading
        flatInstance.loadArea(Area.chunkRange(flatInstance.dimensionType(), 0, 0, viewDiameter+2)).join();

        final TestConnection connection = env.createConnection();
        Collector<ChunkDataPacket> chunkDataPacketCollector = connection.trackIncoming(ChunkDataPacket.class);
        final Player player = connection.connect(flatInstance, new Pos(0.5, 40, 0.5)).join();
        // Initial join
        chunkDataPacketCollector.assertCount(MathUtils.square(viewDiameter));
        player.addPacketToQueue(new ClientTeleportConfirmPacket(player.getLastSentTeleportId()));

        // Move to next chunk
        chunkDataPacketCollector = connection.trackIncoming(ChunkDataPacket.class);
        player.addPacketToQueue(new ClientPlayerPositionPacket(new Vec(-0.5, 40, 0.5), true));
        player.interpretPacketQueue();
        env.tick();
        chunkDataPacketCollector.assertCount(viewDiameter);

        // Move to next chunk
        chunkDataPacketCollector = connection.trackIncoming(ChunkDataPacket.class);
        player.addPacketToQueue(new ClientPlayerPositionPacket(new Vec(-0.5, 40, -0.5), true));
        player.interpretPacketQueue();
        env.tick();
        chunkDataPacketCollector.assertCount(viewDiameter);

        // Move to next chunk
        chunkDataPacketCollector = connection.trackIncoming(ChunkDataPacket.class);
        player.addPacketToQueue(new ClientPlayerPositionPacket(new Vec(0.5, 40, -0.5), true));
        player.interpretPacketQueue();
        env.tick();
        chunkDataPacketCollector.assertCount(viewDiameter);

        // Move to next chunk
        chunkDataPacketCollector = connection.trackIncoming(ChunkDataPacket.class);
        player.addPacketToQueue(new ClientPlayerPositionPacket(new Vec(0.5, 40, 0.5), true));
        player.interpretPacketQueue();
        env.tick();
        chunkDataPacketCollector.assertCount(viewDiameter);

        // Move to next chunk
        chunkDataPacketCollector = connection.trackIncoming(ChunkDataPacket.class);
        player.addPacketToQueue(new ClientPlayerPositionPacket(new Vec(0.5, 40, -0.5), true));
        player.interpretPacketQueue();
        env.tick();
        chunkDataPacketCollector.assertCount(viewDiameter);

        // Move to next chunk
        chunkDataPacketCollector = connection.trackIncoming(ChunkDataPacket.class);
        // Abuse the fact that there is no delta check
        player.addPacketToQueue(new ClientPlayerPositionPacket(new Vec(16.5, 40, -16.5), true));
        player.interpretPacketQueue();
        env.tick();
        chunkDataPacketCollector.assertCount(viewDiameter * 2 - 1);
    }
}
