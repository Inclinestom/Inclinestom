package net.minestom.server.instance.block.rule.vanilla;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class WallPlacementRule extends BlockPlacementRule {

    public WallPlacementRule(Block block) {
        super(block);
    }

    @Override
    public Block blockUpdate(Instance instance, Point blockPosition, Block block) {
        final int x = blockPosition.blockX();
        final int y = blockPosition.blockY();
        final int z = blockPosition.blockZ();

        String east = "none";
        String north = "none";
        String south = "none";
        String up = "true";
        String waterlogged = "false";
        String west = "none";

        if (isBlock(instance, x + 1, y, z)) {
            east = "low";
        }

        if (isBlock(instance, x - 1, y, z)) {
            west = "low";
        }

        if (isBlock(instance, x, y, z + 1)) {
            south = "low";
        }

        if (isBlock(instance, x, y, z - 1)) {
            north = "low";
        }

        return block.withProperties(Map.of(
                "east", east,
                "north", north,
                "south", south,
                "west", west,
                "up", up,
                "waterlogged", waterlogged));
    }

    @Override
    public Block blockPlace(Instance instance,
                            Block block, BlockFace blockFace, Point blockPosition,
                            Player pl) {
        return block;
    }

    private boolean isBlock(Instance instance, int x, int y, int z) {
        return instance.getBlock(x, y, z).isSolid();
    }
}
