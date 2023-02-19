package net.minestom.server.instance;

import net.minestom.server.coordinate.Area;

public interface LoadingRule {
    static LoadingRule playerViewDistance(Instance instance) {
        return new PlayerRadiusLoadingRule(instance);
    }

    Area update(Area area);
}
