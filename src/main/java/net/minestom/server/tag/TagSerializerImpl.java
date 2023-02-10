package net.minestom.server.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.function.Function;

final class TagSerializerImpl {
    public static final TagSerializer<NBTCompound> COMPOUND = new TagSerializer<>() {
        @Override
        public NBTCompound read(TagReadable reader) {
            return ((TagHandler) reader).asCompound();
        }

        @Override
        public void write(TagWritable writer, NBTCompound value) {
            TagNbtSeparator.separate(value, entry -> writer.setTag(entry.tag(), entry.value()));
        }
    };

    static <T> TagSerializer<T> fromCompound(Function<NBTCompound, T> readFunc, Function<T, NBTCompound> writeFunc) {
        return new TagSerializer<>() {
            @Override
            public @Nullable T read(TagReadable reader) {
                final NBTCompound compound = COMPOUND.read(reader);
                return readFunc.apply(compound);
            }

            @Override
            public void write(TagWritable writer, T value) {
                final NBTCompound compound = writeFunc.apply(value);
                COMPOUND.write(writer, compound);
            }
        };
    }
}
