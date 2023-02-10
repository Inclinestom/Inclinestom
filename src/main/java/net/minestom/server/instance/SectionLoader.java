package net.minestom.server.instance;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface SectionLoader {
    static SectionLoader filled(Block block) {
        return pos -> {
            BlockStorage.Mutable storage = BlockStorage.inMemory();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        storage.setBlock(pos.blockX() + x, pos.blockY() + y, pos.blockZ() + z, block);
                    }
                }
            }
            return CompletableFuture.completedFuture(storage);
        };
    }

    /**
     * Attempts to load a section at the given position.
     * @param position the position of the section
     * @return the loaded section, or null if it could not be loaded
     */
    CompletableFuture<@Nullable BlockStorage> load(Vec position);
}
