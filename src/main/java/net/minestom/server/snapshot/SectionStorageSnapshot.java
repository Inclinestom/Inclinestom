package net.minestom.server.snapshot;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;

import java.util.Collection;

public sealed interface SectionStorageSnapshot extends Snapshot, Block.Getter, Biome.Getter
        permits SnapshotImpl.SectionStorage {
    Vec position();

    InstanceSnapshot instance();

    Collection<EntitySnapshot> entities();
}
