package net.minestom.server.instance.block.rule;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BlockPlacementRule {
    private final Block block;

    public BlockPlacementRule(Block block) {
        this.block = block;
    }

    /**
     * Called when the block state id can be updated (for instance if a neighbour block changed).
     *
     * @param instance      the instance of the block
     * @param blockPosition the block position
     * @param currentBlock  the current block
     * @return the updated block
     */
    public abstract Block blockUpdate(Instance instance, Point blockPosition, Block currentBlock);

    /**
     * Called when the block is placed.
     *
     * @param instance      the instance of the block
     * @param block         the block placed
     * @param blockFace     the block face
     * @param blockPosition the block position
     * @param pl            the player who placed the block
     * @return the block to place, {@code null} to cancel
     */
    public abstract @Nullable Block blockPlace(Instance instance,
                                               Block block, BlockFace blockFace, Point blockPosition,
                                               Player pl);

    public Block getBlock() {
        return block;
    }
}
