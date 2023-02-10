package net.minestom.server.instance.block;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.Batch;
import net.minestom.server.registry.ProtocolObject;
import net.minestom.server.registry.Registry;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.*;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Represents a block that can be placed anywhere.
 * Block objects are expected to be reusable and therefore do not
 * retain placement data (e.g. block position)
 * <p>
 * Implementations are expected to be immutable.
 */
public sealed interface Block extends ProtocolObject, TagReadable, Blocks permits BlockImpl {

    /**
     * Creates a new block with the the property {@code property} sets to {@code value}.
     *
     * @param property the property name
     * @param value    the property value
     * @return a new block with its property changed
     * @throws IllegalArgumentException if the property or value are invalid
     */
    @Contract(pure = true)
    Block withProperty(String property, String value);

    /**
     * Changes multiple properties at once.
     * <p>
     * Equivalent to calling {@link #withProperty(String, String)} for each map entry.
     *
     * @param properties map containing all the properties to change
     * @return a new block with its properties changed
     * @throws IllegalArgumentException if the property or value are invalid
     * @see #withProperty(String, String)
     */
    @Contract(pure = true)
    Block withProperties(Map<String, String> properties);

    /**
     * Creates a new block with a tag modified.
     *
     * @param tag   the tag to modify
     * @param value the tag value, null to remove
     * @param <T>   the tag type
     * @return a new block with the modified tag
     */
    @Contract(pure = true)
    <T> Block withTag(Tag<T> tag, @Nullable T value);

    /**
     * Creates a new block with different nbt data.
     *
     * @param compound the new block nbt, null to remove
     * @return a new block with different nbt
     */
    @Contract(pure = true)
    Block withNbt(@Nullable NBTCompound compound);

    /**
     * Creates a new block with the specified {@link BlockHandler handler}.
     *
     * @param handler the new block handler, null to remove
     * @return a new block with the specified handler
     */
    @Contract(pure = true)
    Block withHandler(@Nullable BlockHandler handler);

    /**
     * Returns an unmodifiable view to the block nbt.
     * <p>
     * Be aware that {@link Tag tags} directly affect the block nbt.
     *
     * @return the block nbt, null if not present
     */
    @Contract(pure = true)
    @Nullable NBTCompound nbt();

    @Contract(pure = true)
    default boolean hasNbt() {
        return nbt() != null;
    }

    /**
     * Returns the block handler.
     *
     * @return the block handler, null if not present
     */
    @Contract(pure = true)
    @Nullable BlockHandler handler();

    /**
     * Returns the block properties.
     *
     * @return the block properties map
     */
    @Unmodifiable
    @Contract(pure = true)
    Map<String, String> properties();

    /**
     * Returns a property value from {@link #properties()}.
     *
     * @param property the property name
     * @return the property value, null if not present (due to an invalid property name)
     */
    @Contract(pure = true)
    default String getProperty(String property) {
        return properties().get(property);
    }

    @Contract(pure = true)
    @ApiStatus.Experimental
    Collection<Block> possibleStates();

    /**
     * Returns the block registry.
     * <p>
     * Registry data is directly linked to {@link #stateId()}.
     *
     * @return the block registry
     */
    @Contract(pure = true)
    Registry.BlockEntry registry();

    @Override
    default NamespaceID namespace() {
        return registry().namespace();
    }

    @Override
    default int id() {
        return registry().id();
    }

    default short stateId() {
        return (short) registry().stateId();
    }

    default boolean isAir() {
        return registry().isAir();
    }

    default boolean isSolid() {
        return registry().isSolid();
    }

    default boolean isLiquid() {
        return registry().isLiquid();
    }

    default boolean compare(Block block, Comparator comparator) {
        return comparator.test(this, block);
    }

    default boolean compare(Block block) {
        return compare(block, Comparator.ID);
    }

    static Collection<Block> values() {
        return BlockImpl.values();
    }

    static @Nullable Block fromNamespaceId(String namespaceID) {
        return BlockImpl.getSafe(namespaceID);
    }

    static @Nullable Block fromNamespaceId(NamespaceID namespaceID) {
        return fromNamespaceId(namespaceID.asString());
    }

    static @Nullable Block fromStateId(short stateId) {
        return BlockImpl.getState(stateId);
    }

    static @Nullable Block fromBlockId(int blockId) {
        return BlockImpl.getId(blockId);
    }

    @FunctionalInterface
    interface Comparator extends BiPredicate<Block, Block> {
        Comparator IDENTITY = (b1, b2) -> b1 == b2;

        Comparator ID = (b1, b2) -> b1.id() == b2.id();

        Comparator STATE = (b1, b2) -> b1.stateId() == b2.stateId();
    }

    /**
     * Represents an element which can place blocks at position.
     * <p>
     * Notably used by {@link Instance}, {@link Batch}.
     */
    interface Setter {
        void setBlock(int x, int y, int z, Block block);

        default void setBlock(Point blockPosition, Block block) {
            setBlock(blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ(), block);
        }
    }

    interface Getter {
        @UnknownNullability Block getBlock(int x, int y, int z, Condition condition);

        default @UnknownNullability Block getBlock(Point point, Condition condition) {
            return getBlock(point.blockX(), point.blockY(), point.blockZ(), condition);
        }

        default Block getBlock(int x, int y, int z) {
            return Objects.requireNonNull(getBlock(x, y, z, Condition.NONE));
        }

        default Block getBlock(Point point) {
            return Objects.requireNonNull(getBlock(point, Condition.NONE));
        }

        /**
         * Represents a hint to retrieve blocks more efficiently.
         * Implementing interfaces do not have to honor this.
         */
        @ApiStatus.Experimental
        enum Condition {
            /**
             * Returns a block no matter what.
             * {@link Block#AIR} being the default result.
             */
            NONE,
            /**
             * Hints that the method should return only if the block is cached.
             * <p>
             * Useful if you are only interested in a block handler or nbt.
             */
            CACHED,
            /**
             * Hints that we only care about the block type.
             * <p>
             * Useful if you need to retrieve registry information about the block.
             * Be aware that the returned block may not return the proper handler/nbt.
             */
            TYPE
        }
    }
}
