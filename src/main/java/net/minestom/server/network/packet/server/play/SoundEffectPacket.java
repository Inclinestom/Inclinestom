package net.minestom.server.network.packet.server.play;

import net.kyori.adventure.sound.Sound.Source;
import net.minestom.server.adventure.AdventurePacketConvertor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.*;

public record SoundEffectPacket(int soundId, Source source,
                                int x, int y, int z,
                                float volume, float pitch, long seed) implements ServerPacket {
    public SoundEffectPacket(NetworkBuffer reader) {
        this(reader.read(VAR_INT), reader.readEnum(Source.class),
                reader.read(INT) * 8, reader.read(INT) * 8, reader.read(INT) * 8,
                reader.read(FLOAT), reader.read(FLOAT), reader.read(LONG));
    }

    public SoundEffectPacket(SoundEvent sound, Source source,
                             Point position, float volume, float pitch) {
        this(sound.id(), source, (int) position.x(), (int) position.y(), (int) position.z(),
                volume, pitch, 0);
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(VAR_INT, soundId);
        writer.write(VAR_INT, AdventurePacketConvertor.getSoundSourceValue(source));
        writer.write(INT, x * 8);
        writer.write(INT, y * 8);
        writer.write(INT, z * 8);
        writer.write(FLOAT, volume);
        writer.write(FLOAT, pitch);
        writer.write(LONG, seed);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.SOUND_EFFECT;
    }
}
