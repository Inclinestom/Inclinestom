package net.minestom.server.entity.metadata.minecart;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Metadata;
import org.jetbrains.annotations.NotNull;

public class CommandBlockMinecartMeta extends AbstractMinecartMeta {
    public static final byte OFFSET = AbstractMinecartMeta.MAX_OFFSET;
    public static final byte MAX_OFFSET = OFFSET + 2;

    public CommandBlockMinecartMeta(Entity entity, Metadata metadata) {
        super(entity, metadata);
    }

    public String getCommand() {
        return super.metadata.getIndex(OFFSET, "");
    }

    public void setCommand(String value) {
        super.metadata.setIndex(OFFSET, Metadata.String(value));
    }

    public Component getLastOutput() {
        return super.metadata.getIndex(OFFSET + 1, Component.empty());
    }

    public void setLastOutput(Component value) {
        super.metadata.setIndex(OFFSET + 1, Metadata.Chat(value));
    }

    @Override
    public int getObjectData() {
        return 6;
    }
}
