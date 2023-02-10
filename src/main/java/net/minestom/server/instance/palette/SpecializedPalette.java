package net.minestom.server.instance.palette;

import org.jetbrains.annotations.NotNull;

import java.util.function.IntUnaryOperator;

interface SpecializedPalette extends Palette {
    @Override
    default int bitsPerEntry() {
        throw new UnsupportedOperationException();
    }

    @Override
    default int maxBitsPerEntry() {
        throw new UnsupportedOperationException();
    }

    @Override
    SpecializedPalette clone();

    interface Immutable extends SpecializedPalette {
        @Override
        default void set(int x, int y, int z, int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        default void fill(int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        default void setAll(EntrySupplier supplier) {
            throw new UnsupportedOperationException();
        }

        @Override
        default void replace(int x, int y, int z, IntUnaryOperator operator) {
            throw new UnsupportedOperationException();
        }

        @Override
        default void replaceAll(EntryFunction function) {
            throw new UnsupportedOperationException();
        }
    }
}
