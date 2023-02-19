package net.minestom.server.instance;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.packet.server.play.ChunkDataPacket;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnvTest
public class ChunkViewerIntegrationTest {

    @Test
    public void renderDistance(Env env) {
        final int viewRadius = MinecraftServer.getChunkViewDistance();
        final int count = ChunkUtils.getChunkCount(viewRadius);
        var instance = env.createFlatInstance();
        var connection = env.createConnection();
        // Check initial load
        {
            var tracker = connection.trackIncoming(ChunkDataPacket.class);
            var player = connection.connect(instance, new Pos(0, 40, 0)).join();
            assertEquals(instance, player.getInstance());
            assertEquals(new Pos(0, 40, 0), player.getPosition());
            assertEquals(count, tracker.collect().size());
        }
        // Check chunk#sendChunk
        {
            var tracker = connection.trackIncoming(ChunkDataPacket.class);
            for (int x = -viewRadius; x <= viewRadius; x++) {
                for (int z = -viewRadius; z <= viewRadius; z++) {
                    Area chunkArea = Area.chunk(instance.dimensionType(), x, z);
                    ChunkDataPacket packet = instance.chunkPacket(x, z);
                    instance.viewers(chunkArea).sendPacketToViewers(packet);
                }
            }
            assertEquals(count, tracker.collect().size());
        }
    }
}
