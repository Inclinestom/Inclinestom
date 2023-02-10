package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.block.Block;

import java.util.function.Consumer;

public interface BlockStorage extends Block.Getter {

    Area area();

    static Mutable inMemory() {
        return new InMemoryBlockStorage();
    }

    interface Mutable extends BlockStorage, Block.Setter {
        void mutate(Consumer<Block.Setter> mutator);
        void clear(Area area);
    }

    static Union union() {
        return new UnionBlockStorage();
    }

    interface Union extends BlockStorage, Mutable {
        void add(BlockStorage storage);
        boolean remove(BlockStorage storage);
    }

    static BlockStorage filled(Block block) {
        return new FilledBlockStorage(block);
    }

    static BlockStorage empty() {
        return new EmptyBlockStorage();
    }

    static BlockStorage view(BlockStorage blockStorage, Area area) {
        return new ViewBlockStorage(blockStorage, area);
    }
}
