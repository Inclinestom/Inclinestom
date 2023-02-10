package net.minestom.server.thread;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public class MinestomThread extends Thread {
    public MinestomThread(String name) {
        super(name);
    }
}
