package net.minestom.server.snapshot;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.utils.collection.IntMappedArray;
import net.minestom.server.utils.collection.MappedCollection;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static net.minestom.server.utils.chunk.ChunkUtils.*;

@ApiStatus.Internal
public final class SnapshotImpl {
    public record Server(Collection<InstanceSnapshot> instances,
                         Int2ObjectOpenHashMap<AtomicReference<EntitySnapshot>> entityRefs) implements ServerSnapshot {
        @Override
        public Collection<EntitySnapshot> entities() {
            return MappedCollection.plainReferences(entityRefs.values());
        }

        @Override
        public @UnknownNullability EntitySnapshot entity(int id) {
            var ref = entityRefs.get(id);
            return ref != null ? ref.getPlain() : null;
        }
    }

    public record Instance(AtomicReference<ServerSnapshot> serverRef,
                           DimensionType dimensionType, long worldAge, long time,
                           Map<Long, AtomicReference<SectionStorageSnapshot>> chunksMap,
                           int[] entitiesIds,
                           TagReadable tagReadable) implements InstanceSnapshot {
        @Override
        public @Nullable SectionStorageSnapshot chunk(int chunkX, int chunkZ) {
            var ref = chunksMap.get(getChunkIndex(chunkX, chunkZ));
            return Objects.requireNonNull(ref, "Chunk not found").getPlain();
        }

        @Override
        public Collection<SectionStorageSnapshot> chunks() {
            return MappedCollection.plainReferences(chunksMap.values());
        }

        @Override
        public Collection<EntitySnapshot> entities() {
            return new IntMappedArray<>(entitiesIds, id -> server().entity(id));
        }

        @Override
        public ServerSnapshot server() {
            return serverRef.getPlain();
        }

        @Override
        public <T> @UnknownNullability T getTag(Tag<T> tag) {
            return tagReadable.getTag(tag);
        }
    }

    public record SectionStorage(int minSection, int chunkX, int chunkZ,
                                 Section[] sections,
                                 Int2ObjectOpenHashMap<Block> blockEntries,
                                 int[] entitiesIds,
                                 AtomicReference<InstanceSnapshot> instanceRef) implements SectionStorageSnapshot {
        @Override
        public @UnknownNullability Block getBlock(int x, int y, int z, Condition condition) {
            // Verify if the block object is present
            if (condition != Condition.TYPE) {
                final Block entry = !blockEntries.isEmpty() ?
                        blockEntries.get(getBlockIndex(x, y, z)) : null;
                if (entry != null || condition == Condition.CACHED) {
                    return entry;
                }
            }
            // Retrieve the block from state id
            final Section section = sections[getChunkCoordinate(y) - minSection];
            final int blockStateId = section.blockPalette()
                    .get(toSectionRelativeCoordinate(x), toSectionRelativeCoordinate(y), toSectionRelativeCoordinate(z));
            return Objects.requireNonNullElse(Block.fromStateId((short) blockStateId), Block.AIR);
        }

        @Override
        public Biome getBiome(int x, int y, int z) {
            final Section section = sections[getChunkCoordinate(y) - minSection];
            final int id = section.biomePalette()
                    .get(toSectionRelativeCoordinate(x) / 4, toSectionRelativeCoordinate(y) / 4, toSectionRelativeCoordinate(z) / 4);
            return MinecraftServer.getBiomeManager().getById(id);
        }

        @Override
        public InstanceSnapshot instance() {
            return instanceRef.getPlain();
        }

        @Override
        public Collection<EntitySnapshot> entities() {
            return new IntMappedArray<>(entitiesIds, id -> instance().server().entity(id));
        }
    }

    public record Entity(EntityType type, UUID uuid, int id, Pos position, Vec velocity,
                         AtomicReference<InstanceSnapshot> instanceRef, int chunkX, int chunkZ,
                         int[] viewersId, int[] passengersId, int vehicleId,
                         TagReadable tagReadable) implements EntitySnapshot {
        @Override
        public <T> @UnknownNullability T getTag(Tag<T> tag) {
            return tagReadable.getTag(tag);
        }

        @Override
        public InstanceSnapshot instance() {
            return instanceRef.getPlain();
        }

        @Override
        public SectionStorageSnapshot chunk() {
            return Objects.requireNonNull(instance().chunk(chunkX, chunkZ));
        }

        @Override
        public Collection<PlayerSnapshot> viewers() {
            return new IntMappedArray<>(viewersId, id -> (PlayerSnapshot) instance().server().entity(id));
        }

        @Override
        public Collection<EntitySnapshot> passengers() {
            return new IntMappedArray<>(passengersId, id -> instance().server().entity(id));
        }

        @Override
        public @Nullable EntitySnapshot vehicle() {
            if (vehicleId == -1) return null;
            return instance().server().entity(vehicleId);
        }
    }

    public record Player(EntitySnapshot snapshot, String username,
                         GameMode gameMode) implements PlayerSnapshot {
        @Override
        public EntityType type() {
            return snapshot.type();
        }

        @Override
        public UUID uuid() {
            return snapshot.uuid();
        }

        @Override
        public int id() {
            return snapshot.id();
        }

        @Override
        public Pos position() {
            return snapshot.position();
        }

        @Override
        public Vec velocity() {
            return snapshot.velocity();
        }

        @Override
        public InstanceSnapshot instance() {
            return snapshot.instance();
        }

        @Override
        public SectionStorageSnapshot chunk() {
            return snapshot.chunk();
        }

        @Override
        public Collection<PlayerSnapshot> viewers() {
            return snapshot.viewers();
        }

        @Override
        public Collection<EntitySnapshot> passengers() {
            return snapshot.passengers();
        }

        @Override
        public @Nullable EntitySnapshot vehicle() {
            return snapshot.vehicle();
        }

        @Override
        public <T> @UnknownNullability T getTag(Tag<T> tag) {
            return snapshot.getTag(tag);
        }
    }
}
