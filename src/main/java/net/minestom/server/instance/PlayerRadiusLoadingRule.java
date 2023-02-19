package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.AreaUtils;

import java.util.ArrayList;
import java.util.List;

record PlayerRadiusLoadingRule(Instance instance) implements LoadingRule {
    @Override
    public Area update(Area area) {
        List<Area> loaded = new ArrayList<>();
        for (Player player : instance.players()) {
            AreaUtils.forEachChunk(player.viewArea(), (x, z) -> {
                Area chunk = Area.chunk(instance.dimensionType(), x, z);
                loaded.add(chunk);
            });
        }
        return Area.union(loaded);
    }
}
