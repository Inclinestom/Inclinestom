package net.minestom.server.event.instance;

import net.minestom.server.coordinate.Area;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.storage.WorldView;

/**
 * Called when a chunk in an instance is unloaded.
 */
public class InstanceAreaUnloadEvent implements InstanceEvent {

    private final Instance instance;
    private final Area area;
    private final WorldView chunk;

    public InstanceAreaUnloadEvent(Instance instance, Area area, WorldView chunk) {
        this.instance = instance;
        this.area = area;
        this.chunk = chunk;
    }

    @Override
    public Instance getInstance() {
        return instance;
    }

    /**
     * Gets the area.
     */
    public Area getArea() {
        return area;
    }

    /**
     * Gets the world view.
     */
    public WorldView getWorldView() {
        return chunk;
    }
}
