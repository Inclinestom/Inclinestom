package net.minestom.server.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an element which can write {@link Tag tags}.
 */
public interface TagWritable {

    /**
     * Writes the specified type.
     *
     * @param tag   the tag to write
     * @param value the tag value, null to remove
     * @param <T>   the tag type
     */
    <T> void setTag(Tag<T> tag, @Nullable T value);

    default void removeTag(Tag<?> tag) {
        setTag(tag, null);
    }
}
