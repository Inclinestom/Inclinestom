package net.minestom.server.entity.pathfinding;

import com.extollit.gaming.ai.path.model.IBlockObject;
import com.extollit.gaming.ai.path.model.IColumnarSpace;
import com.extollit.gaming.ai.path.model.IInstanceSpace;
import net.minestom.server.coordinate.Area;
import net.minestom.server.instance.storage.WorldView;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PFInstanceSpace implements IInstanceSpace {
    private final Instance instance;
    private final Map<WorldView, PFColumnarSpace> chunkSpaceMap = new ConcurrentHashMap<>();

    public PFInstanceSpace(Instance instance) {
        this.instance = instance;
    }

    @Override
    public IBlockObject blockObjectAt(int x, int y, int z) {
        final Block block = instance.getBlock(x, y, z);
        return PFBlock.get(block);
    }

    @Override
    public IColumnarSpace columnarSpaceAt(int cx, int cz) {
        DimensionType dimensionType = instance.dimensionType();
        Area chunk = Area.chunk(dimensionType, cx, cz);
        final WorldView view = instance.worldView(chunk);
        if (view == null) return null;
        return chunkSpaceMap.computeIfAbsent(view, c -> new PFColumnarSpace(this, c));
    }

    public Instance getInstance() {
        return instance;
    }
}
