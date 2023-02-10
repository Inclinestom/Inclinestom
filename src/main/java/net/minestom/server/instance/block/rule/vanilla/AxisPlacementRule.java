package net.minestom.server.instance.block.rule.vanilla;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

public class AxisPlacementRule extends BlockPlacementRule {

    public AxisPlacementRule(Block block) {
        super(block);
    }

    @Override
    public Block blockUpdate(Instance instance, Point blockPosition, Block block) {
        return block;
    }

    @Override
    public Block blockPlace(Instance instance,
                            Block block, BlockFace blockFace, Point blockPosition,
                            Player pl) {
        String axis = "y";
        if (blockFace == BlockFace.WEST || blockFace == BlockFace.EAST) {
            axis = "x";
        } else if (blockFace == BlockFace.SOUTH || blockFace == BlockFace.NORTH) {
            axis = "z";
        }
        return block.withProperty("axis", axis);
    }
}
