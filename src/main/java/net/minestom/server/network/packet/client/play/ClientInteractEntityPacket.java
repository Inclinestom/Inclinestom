package net.minestom.server.network.packet.client.play;

import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.utils.binary.Writeable;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.*;

public record ClientInteractEntityPacket(int targetId, Type type, boolean sneaking) implements ClientPacket {
    public ClientInteractEntityPacket(NetworkBuffer reader) {
        this(reader.read(VAR_INT), switch (reader.read(VAR_INT)) {
            case 0 -> new Interact(reader);
            case 1 -> new Attack();
            case 2 -> new InteractAt(reader);
            default -> throw new RuntimeException("Unknown action id");
        }, reader.read(BOOLEAN));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(VAR_INT, targetId);
        writer.write(VAR_INT, type.id());
        writer.write(type);
        writer.write(BOOLEAN, sneaking);
    }

    public sealed interface Type extends Writer
            permits Interact, Attack, InteractAt {
        int id();
    }

    public record Interact(Player.Hand hand) implements Type {
        public Interact(NetworkBuffer reader) {
            this(reader.readEnum(Player.Hand.class));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.writeEnum(Player.Hand.class, hand);
        }

        @Override
        public int id() {
            return 0;
        }
    }

    public record Attack() implements Type {
        @Override
        public void write(NetworkBuffer writer) {
            // Empty
        }

        @Override
        public int id() {
            return 1;
        }
    }

    public record InteractAt(float targetX, float targetY, float targetZ,
                             Player.Hand hand) implements Type {
        public InteractAt(NetworkBuffer reader) {
            this(reader.read(FLOAT), reader.read(FLOAT), reader.read(FLOAT),
                    reader.readEnum(Player.Hand.class));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(FLOAT, targetX);
            writer.write(FLOAT, targetY);
            writer.write(FLOAT, targetZ);
            writer.writeEnum(Player.Hand.class, hand);
        }

        @Override
        public int id() {
            return 2;
        }
    }
}
