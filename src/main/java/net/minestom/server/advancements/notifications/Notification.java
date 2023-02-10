package net.minestom.server.advancements.notifications;

import net.kyori.adventure.text.Component;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a message which can be sent using the {@link NotificationCenter}.
 */
public record Notification(Component title, FrameType frameType, ItemStack icon) {
    public Notification(Component title, FrameType frameType, Material icon) {
        this(title, frameType, ItemStack.of(icon));
    }

    @Deprecated
    public Component getTitle() {
        return title;
    }

    @Deprecated
    public FrameType getFrameType() {
        return frameType;
    }
}
