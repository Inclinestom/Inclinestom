package net.minestom.server.particle;

import net.minestom.server.registry.ProtocolObject;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public sealed interface Particle extends ProtocolObject, Particles permits ParticleImpl {

    static Collection<Particle> values() {
        return ParticleImpl.values();
    }

    static @Nullable Particle fromNamespaceId(String namespaceID) {
        return ParticleImpl.getSafe(namespaceID);
    }

    static @Nullable Particle fromNamespaceId(NamespaceID namespaceID) {
        return fromNamespaceId(namespaceID.asString());
    }

    static @Nullable Particle fromId(int id) {
        return ParticleImpl.getId(id);
    }
}
