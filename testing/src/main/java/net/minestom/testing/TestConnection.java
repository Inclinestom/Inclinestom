package net.minestom.testing;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface TestConnection {
    CompletableFuture<Player> connect(Instance instance, Pos pos);

    <T extends ServerPacket> Collector<T> trackIncoming(Class<T> type);

    default Collector<ServerPacket> trackIncoming() {
        return trackIncoming(ServerPacket.class);
    }
}
