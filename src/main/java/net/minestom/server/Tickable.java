package net.minestom.server;

import java.util.concurrent.CompletableFuture;

/**
 * Represents an element which is ticked at a regular interval.
 */
public interface Tickable {

    /**
     * Ticks this element.
     *
     * @param time the time of the tick in milliseconds
     */
    CompletableFuture<Void> tick(long time);
}
