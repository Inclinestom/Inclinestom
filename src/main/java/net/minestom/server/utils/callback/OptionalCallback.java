package net.minestom.server.utils.callback;

import net.minestom.server.instance.storage.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * Convenient class to execute callbacks which can be null.
 */
public class OptionalCallback {

    /**
     * Executes an optional {@link Runnable}.
     *
     * @param callback the optional runnable, can be null
     */
    public static void execute(@Nullable Runnable callback) {
        if (callback != null) {
            callback.run();
        }
    }
}
