package net.minestom.server.utils.chunk;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.storage.WorldView;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.play.ChunkDataPacket;
import net.minestom.server.network.packet.server.play.data.ChunkData;
import net.minestom.server.network.packet.server.play.data.LightData;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.utils.ObjectPool;
import net.minestom.server.utils.function.IntegerBiConsumer;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@ApiStatus.Internal
public final class ChunkUtils {

    private ChunkUtils() {
    }

    /**
     * Executes {@link Instance#loadArea(Area)} for the array of chunks {@code chunks}
     * with multiple callbacks, {@code eachCallback} which is executed each time a new chunk is loaded and
     * {@code endCallback} when all the chunks in the array have been loaded.
     *
     * @param instance     the instance to load the chunks from
     * @param chunks       the chunks to loaded, long value from {@link #getChunkIndex(int, int)}
     * @param eachCallback the optional callback when a chunk get loaded
     * @return a {@link CompletableFuture} completed once all chunks have been processed
     */
    public static CompletableFuture<Void> loadAll(Instance instance, long [] chunks,
                                                                   @Nullable Consumer<WorldView> eachCallback) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        AtomicInteger counter = new AtomicInteger(0);
        for (long visibleWorldView : chunks) {
            // WARNING: if autoload is disabled and no chunks are loaded beforehand, player will be stuck.
            Area chunkArea = Area.chunk(instance.dimensionType(), getChunkCoordX(visibleWorldView), getChunkCoordZ(visibleWorldView));
            instance.loadArea(chunkArea)
                    .thenAccept((chunk) -> {
                        if (eachCallback != null) eachCallback.accept(chunk);
                        if (counter.incrementAndGet() == chunks.length) {
                            // This is the last chunk to be loaded , spawn player
                            completableFuture.complete(null);
                        }
                    });
        }
        return completableFuture;
    }

    /**
     * Gets if a chunk is loaded.
     *
     * @param instance the instance to check
     * @param x        instance X coordinate
     * @param z        instance Z coordinate
     * @return true if the chunk is loaded, false otherwise
     */
    public static boolean isLoaded(Instance instance, double x, double z) {
        final Area chunk = Area.chunk(instance.dimensionType(), getSectionCoordinate(x), getSectionCoordinate(z));
        return instance.isAreaLoaded(chunk);
    }

    public static boolean isLoaded(Instance instance, Point point) {
        return isLoaded(instance, point.x(), point.z());
    }

    /**
     * @param xz the instance coordinate to convert
     * @return the chunk X or Z based on the argument
     */
    public static int getSectionCoordinate(double xz) {
        return getSectionCoordinate((int) Math.floor(xz));
    }

    public static int getSectionCoordinate(int xz) {
        // Assume chunk/section size being 16 (4 bits)
        return xz >> 4;
    }

    /**
     * Gets the chunk index of chunk coordinates.
     * <p>
     * Used when you want to store a chunk somewhere without using a reference to the whole object
     * (as this can lead to memory leaks).
     *
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return a number storing the chunk X and Z
     */
    public static long getChunkIndex(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xffffffffL);
    }

    public static long getChunkIndex(Point point) {
        return getChunkIndex(point.sectionX(), point.sectionZ());
    }

    /**
     * Converts a chunk index to its chunk X position.
     *
     * @param index the chunk index computed by {@link #getChunkIndex(int, int)}
     * @return the chunk X based on the index
     */
    public static int getChunkCoordX(long index) {
        return (int) (index >> 32);
    }

    /**
     * Converts a chunk index to its chunk Z position.
     *
     * @param index the chunk index computed by {@link #getChunkIndex(int, int)}
     * @return the chunk Z based on the index
     */
    public static int getChunkCoordZ(long index) {
        return (int) index;
    }

    public static int getChunkCount(int range) {
        if (range < 0) {
            throw new IllegalArgumentException("Range cannot be negative");
        }
        final int square = range * 2 + 1;
        return square * square;
    }

    public static void forDifferingChunksInRange(int newWorldViewX, int newWorldViewZ,
                                                 int oldWorldViewX, int oldWorldViewZ,
                                                 int range, IntegerBiConsumer callback) {
        for (int x = newWorldViewX - range; x <= newWorldViewX + range; x++) {
            for (int z = newWorldViewZ - range; z <= newWorldViewZ + range; z++) {
                if (Math.abs(x - oldWorldViewX) > range || Math.abs(z - oldWorldViewZ) > range) {
                    callback.accept(x, z);
                }
            }
        }
    }

    public static void forDifferingChunksInRange(int newWorldViewX, int newWorldViewZ,
                                                 int oldWorldViewX, int oldWorldViewZ,
                                                 int range,
                                                 IntegerBiConsumer newCallback, IntegerBiConsumer oldCallback) {
        // Find the new chunks
        forDifferingChunksInRange(newWorldViewX, newWorldViewZ, oldWorldViewX, oldWorldViewZ, range, newCallback);
        // Find the old chunks
        forDifferingChunksInRange(oldWorldViewX, oldWorldViewZ, newWorldViewX, newWorldViewZ, range, oldCallback);
    }

    public static void forChunksInRange(int chunkX, int chunkZ, int range, IntegerBiConsumer consumer) {
        for (int x = -range; x <= range; ++x) {
            for (int z = -range; z <= range; ++z) {
                consumer.accept(chunkX + x, chunkZ + z);
            }
        }
    }

    public static void forChunksInRange(Point point, int range, IntegerBiConsumer consumer) {
        forChunksInRange(point.sectionX(), point.sectionZ(), range, consumer);
    }

    /**
     * Gets the block index of a position.
     *
     * @param x the block X
     * @param y the block Y
     * @param z the block Z
     * @return an index which can be used to store and retrieve later data linked to a block position
     */
    public static int getBlockIndex(int x, int y, int z) {
        x = x % Instance.SECTION_SIZE;
        z = z % Instance.SECTION_SIZE;

        int index = x & 0xF; // 4 bits
        if (y > 0) {
            index |= (y << 4) & 0x07FFFFF0; // 23 bits (24th bit is always 0 because y is positive)
        } else {
            index |= ((-y) << 4) & 0x7FFFFF0; // Make positive and use 23 bits
            index |= 1 << 27; // Set negative sign at 24th bit
        }
        index |= (z << 28) & 0xF0000000; // 4 bits
        return index;
    }

    /**
     * @param index  an index computed from {@link #getBlockIndex(int, int, int)}
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return the instance position of the block located in {@code index}
     */
    public static Point getBlockPosition(int index, int chunkX, int chunkZ) {
        final int x = blockIndexToChunkPositionX(index) + Instance.SECTION_SIZE * chunkX;
        final int y = blockIndexToChunkPositionY(index);
        final int z = blockIndexToChunkPositionZ(index) + Instance.SECTION_SIZE * chunkZ;
        return new Vec(x, y, z);
    }

    /**
     * Converts a block index to a chunk position X.
     *
     * @param index an index computed from {@link #getBlockIndex(int, int, int)}
     * @return the chunk position X (O-15) of the specified index
     */
    public static int blockIndexToChunkPositionX(int index) {
        return index & 0xF; // 0-4 bits
    }

    /**
     * Converts a block index to a chunk position Y.
     *
     * @param index an index computed from {@link #getBlockIndex(int, int, int)}
     * @return the chunk position Y of the specified index
     */
    public static int blockIndexToChunkPositionY(int index) {
        int y = (index & 0x07FFFFF0) >>> 4;
        if (((index >>> 27) & 1) == 1) y = -y; // Sign bit set, invert sign
        return y; // 4-28 bits
    }

    /**
     * Converts a block index to a chunk position Z.
     *
     * @param index an index computed from {@link #getBlockIndex(int, int, int)}
     * @return the chunk position Z (O-15) of the specified index
     */
    public static int blockIndexToChunkPositionZ(int index) {
        return (index >> 28) & 0xF; // 28-32 bits
    }

    /**
     * Converts a global coordinate value to a section coordinate
     *
     * @param xyz global coordinate
     * @return section coordinate
     */
    public static int toSectionRelativeCoordinate(int xyz) {
        return xyz & 0xF;
    }

    public static int floorSection(int coordinate) {
        return coordinate - (coordinate & 0xF);
    }

    public static int ceilSection(int coordinate) {
        return ((coordinate - 1) | 15) + 1;
    }

    /**
     * Creates a chunk packet from a {@link WorldView}.
     * <p>
     * NOTE: This method does not do any checks on the chunk data, it is up to the caller to ensure
     * that all necessary chunk data is present.
     * </p>
     *
     * @param blockStorage the block storage
     * @param chunkX       the chunk X
     * @param chunkZ       the chunk Z
     * @return a chunk packet
     */
    public static ChunkDataPacket chunkPacket(WorldView blockStorage, DimensionType dimensionType, int chunkX, int chunkZ) {
        return new ChunkDataPacket(chunkX, chunkZ,
                chunkData(blockStorage, dimensionType, chunkX, chunkZ),
                lightData(blockStorage, dimensionType, chunkX, chunkZ));
    }

    private static ChunkData chunkData(WorldView blockStorage, DimensionType dimensionType, int chunkX, int chunkZ) {
        Area chunkArea = Area.chunk(dimensionType, chunkX, chunkZ);
        final NBTCompound heightmapsNBT;
        // TODO: don't hardcode heightmaps
        // Heightmap
        {
            int dimensionHeight = dimensionType.getHeight();
            int[] motionBlocking = new int[16 * 16];
            int[] worldSurface = new int[16 * 16];
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    motionBlocking[x + z * 16] = 0;
                    worldSurface[x + z * 16] = dimensionHeight - 1;
                }
            }
            final int bitsForHeight = MathUtils.bitsToRepresent(dimensionHeight);
            heightmapsNBT = NBT.Compound(Map.of(
                    "MOTION_BLOCKING", NBT.LongArray(encodeBlocks(motionBlocking, bitsForHeight)),
                    "WORLD_SURFACE", NBT.LongArray(encodeBlocks(worldSurface, bitsForHeight))));
        }
        // Data
        final byte[] data = ObjectPool.PACKET_POOL.use(buffer ->
            NetworkBuffer.makeArray(networkBuffer -> {
                int minSection = dimensionType.getMinY() / Instance.SECTION_SIZE;
                int maxSection = dimensionType.getMaxY() / Instance.SECTION_SIZE;
                for (int section = minSection; section <= maxSection; section++) {
                    createSection(blockStorage, chunkX, section, chunkZ).write(networkBuffer);
                }
            })
        );
        Int2ObjectOpenHashMap<Block> entries = new Int2ObjectOpenHashMap<>();
        for (Point point : chunkArea) {
            Block block = blockStorage.getBlock(point);
            entries.put(block.stateId(), block);
        }
        return new ChunkData(heightmapsNBT, data, entries);
    }

    private static Section createSection(WorldView view, int sectionX, int sectionY, int sectionZ) {
        Section section = new Section();
        for (int x = 0; x < Instance.SECTION_SIZE; x++) {
            for (int z = 0; z < Instance.SECTION_SIZE; z++) {
                for (int y = 0; y < Instance.SECTION_SIZE; y++) {
                    int blockX = x + sectionX * Instance.SECTION_SIZE;
                    int blockY = y + sectionY * Instance.SECTION_SIZE;
                    int blockZ = z + sectionZ * Instance.SECTION_SIZE;
                    Block block = view.area().contains(blockX, blockY, blockZ) ?
                            view.getBlock(blockX, blockY, blockZ) : null;
                    if (block == null) block = Block.AIR;
                    section.blockPalette().set(x, y, z, block.stateId());
                    if (x % 4 == 0 && y % 4 == 0 && z % 4 == 0) {
                        Biome biome = view.area().contains(blockX, blockY, blockZ) ?
                                view.getBiome(blockX, blockY, blockZ) : null;
                        if (biome == null) biome = Biome.PLAINS;
                        section.biomePalette().set(x / 4, y / 4, z / 4, biome.id());
                    }
                    // TODO: Lighting
                }
            }
        }
        return section;
    }

    private static final int[] MAGIC = {
            -1, -1, 0, Integer.MIN_VALUE, 0, 0, 1431655765, 1431655765, 0, Integer.MIN_VALUE,
            0, 1, 858993459, 858993459, 0, 715827882, 715827882, 0, 613566756, 613566756,
            0, Integer.MIN_VALUE, 0, 2, 477218588, 477218588, 0, 429496729, 429496729, 0,
            390451572, 390451572, 0, 357913941, 357913941, 0, 330382099, 330382099, 0, 306783378,
            306783378, 0, 286331153, 286331153, 0, Integer.MIN_VALUE, 0, 3, 252645135, 252645135,
            0, 238609294, 238609294, 0, 226050910, 226050910, 0, 214748364, 214748364, 0,
            204522252, 204522252, 0, 195225786, 195225786, 0, 186737708, 186737708, 0, 178956970,
            178956970, 0, 171798691, 171798691, 0, 165191049, 165191049, 0, 159072862, 159072862,
            0, 153391689, 153391689, 0, 148102320, 148102320, 0, 143165576, 143165576, 0,
            138547332, 138547332, 0, Integer.MIN_VALUE, 0, 4, 130150524, 130150524, 0, 126322567,
            126322567, 0, 122713351, 122713351, 0, 119304647, 119304647, 0, 116080197, 116080197,
            0, 113025455, 113025455, 0, 110127366, 110127366, 0, 107374182, 107374182, 0,
            104755299, 104755299, 0, 102261126, 102261126, 0, 99882960, 99882960, 0, 97612893,
            97612893, 0, 95443717, 95443717, 0, 93368854, 93368854, 0, 91382282, 91382282,
            0, 89478485, 89478485, 0, 87652393, 87652393, 0, 85899345, 85899345, 0,
            84215045, 84215045, 0, 82595524, 82595524, 0, 81037118, 81037118, 0, 79536431,
            79536431, 0, 78090314, 78090314, 0, 76695844, 76695844, 0, 75350303, 75350303,
            0, 74051160, 74051160, 0, 72796055, 72796055, 0, 71582788, 71582788, 0,
            70409299, 70409299, 0, 69273666, 69273666, 0, 68174084, 68174084, 0, Integer.MIN_VALUE,
            0, 5};

    private static long[] encodeBlocks(int[] blocks, int bitsPerEntry) {
        final long maxEntryValue = (1L << bitsPerEntry) - 1;
        final char valuesPerLong = (char) (64 / bitsPerEntry);
        final int magicIndex = 3 * (valuesPerLong - 1);
        final long divideMul = Integer.toUnsignedLong(MAGIC[magicIndex]);
        final long divideAdd = Integer.toUnsignedLong(MAGIC[magicIndex + 1]);
        final int divideShift = MAGIC[magicIndex + 2];
        final int size = (blocks.length + valuesPerLong - 1) / valuesPerLong;

        long[] data = new long[size];

        for (int i = 0; i < blocks.length; i++) {
            final long value = blocks[i];
            final int cellIndex = (int) (i * divideMul + divideAdd >> 32L >> divideShift);
            final int bitIndex = (i - cellIndex * valuesPerLong) * bitsPerEntry;
            data[cellIndex] = data[cellIndex] & ~(maxEntryValue << bitIndex) | (value & maxEntryValue) << bitIndex;
        }

        return data;
    }

    private static LightData lightData(WorldView blockStorage, DimensionType dimensionType, int chunkX, int chunkZ) {
        BitSet skyMask = new BitSet();
        BitSet blockMask = new BitSet();
        BitSet emptySkyMask = new BitSet();
        BitSet emptyBlockMask = new BitSet();
        List<byte[]> skyLights = new ArrayList<>();
        List<byte[]> blockLights = new ArrayList<>();

        int minSection = dimensionType.getMinY() / Instance.SECTION_SIZE;
        int maxSection = dimensionType.getMaxY() / Instance.SECTION_SIZE;

        int index = 0;
        for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
            Section section = createSection(blockStorage, chunkX, sectionY, chunkZ);
            index++;
            final byte[] skyLight = section.getSkyLight();
            final byte[] blockLight = section.getBlockLight();
            if (skyLight.length != 0) {
                skyLights.add(skyLight);
                skyMask.set(index);
            } else {
                emptySkyMask.set(index);
            }
            if (blockLight.length != 0) {
                blockLights.add(blockLight);
                blockMask.set(index);
            } else {
                emptyBlockMask.set(index);
            }
        }
        return new LightData(true,
                skyMask, blockMask,
                emptySkyMask, emptyBlockMask,
                skyLights, blockLights);
    }

    public static Map<Integer, Set<Point>> groupByY(Collection<Point> sections) {
        Map<Integer, Set<Point>> grouped = new HashMap<>();
        for (Point section : sections) {
            grouped.computeIfAbsent(section.sectionY(), k -> new HashSet<>()).add(section);
        }
        return Collections.unmodifiableMap(grouped);
    }
}
