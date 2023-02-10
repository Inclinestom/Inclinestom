package net.minestom.server.network.packet.server.play;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.adventure.AdventurePacketConvertor;
import net.minestom.server.adventure.ComponentHolder;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ComponentHoldingServerPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static net.minestom.server.network.NetworkBuffer.*;

public record BossBarPacket(UUID uuid, Action action) implements ComponentHoldingServerPacket {
    public BossBarPacket(NetworkBuffer reader) {
        this(reader.read(NetworkBuffer.UUID), switch (reader.read(VAR_INT)) {
            case 0 -> new AddAction(reader);
            case 1 -> new RemoveAction();
            case 2 -> new UpdateHealthAction(reader);
            case 3 -> new UpdateTitleAction(reader);
            case 4 -> new UpdateStyleAction(reader);
            case 5 -> new UpdateFlagsAction(reader);
            default -> throw new RuntimeException("Unknown action id");
        });
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(NetworkBuffer.UUID, uuid);
        writer.write(VAR_INT, action.id());
        writer.write(action);
    }

    @Override
    public Collection<Component> components() {
        return this.action instanceof ComponentHolder<?> holder
                ? holder.components()
                : List.of();
    }

    @Override
    public ServerPacket copyWithOperator(UnaryOperator<Component> operator) {
        return this.action instanceof ComponentHolder<?> holder
                ? new BossBarPacket(this.uuid, (Action) holder.copyWithOperator(operator))
                : this;
    }

    public sealed interface Action extends NetworkBuffer.Writer
            permits AddAction, RemoveAction, UpdateHealthAction, UpdateTitleAction, UpdateStyleAction, UpdateFlagsAction {
        int id();
    }

    public record AddAction(Component title, float health, BossBar.Color color,
                            BossBar.Overlay overlay,
                            byte flags) implements Action, ComponentHolder<AddAction> {
        public AddAction(BossBar bar) {
            this(bar.name(), bar.progress(), bar.color(), bar.overlay(),
                    AdventurePacketConvertor.getBossBarFlagValue(bar.flags()));
        }

        public AddAction(NetworkBuffer reader) {
            this(reader.read(COMPONENT), reader.read(FLOAT),
                    BossBar.Color.values()[reader.read(VAR_INT)],
                    BossBar.Overlay.values()[reader.read(VAR_INT)], reader.read(BYTE));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(COMPONENT, title);
            writer.write(FLOAT, health);
            writer.write(VAR_INT, AdventurePacketConvertor.getBossBarColorValue(color));
            writer.write(VAR_INT, AdventurePacketConvertor.getBossBarOverlayValue(overlay));
            writer.write(BYTE, flags);
        }

        @Override
        public int id() {
            return 0;
        }

        @Override
        public Collection<Component> components() {
            return List.of(this.title);
        }

        @Override
        public AddAction copyWithOperator(UnaryOperator<Component> operator) {
            return new AddAction(operator.apply(this.title), this.health, this.color, this.overlay, this.flags);
        }
    }

    public record RemoveAction() implements Action {
        @Override
        public void write(NetworkBuffer writer) {
        }

        @Override
        public int id() {
            return 1;
        }
    }

    public record UpdateHealthAction(float health) implements Action {
        public UpdateHealthAction(BossBar bar) {
            this(bar.progress());
        }

        public UpdateHealthAction(NetworkBuffer reader) {
            this(reader.read(FLOAT));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(FLOAT, health);
        }

        @Override
        public int id() {
            return 2;
        }
    }

    public record UpdateTitleAction(Component title) implements Action, ComponentHolder<UpdateTitleAction> {
        public UpdateTitleAction(BossBar bar) {
            this(bar.name());
        }

        public UpdateTitleAction(NetworkBuffer reader) {
            this(reader.read(COMPONENT));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(COMPONENT, title);
        }

        @Override
        public int id() {
            return 3;
        }

        @Override
        public Collection<Component> components() {
            return List.of(this.title);
        }

        @Override
        public UpdateTitleAction copyWithOperator(UnaryOperator<Component> operator) {
            return new UpdateTitleAction(operator.apply(this.title));
        }
    }

    public record UpdateStyleAction(BossBar.Color color,
                                    BossBar.Overlay overlay) implements Action {
        public UpdateStyleAction(BossBar bar) {
            this(bar.color(), bar.overlay());
        }

        public UpdateStyleAction(NetworkBuffer reader) {
            this(BossBar.Color.values()[reader.read(VAR_INT)], BossBar.Overlay.values()[reader.read(VAR_INT)]);
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(VAR_INT, AdventurePacketConvertor.getBossBarColorValue(color));
            writer.write(VAR_INT, AdventurePacketConvertor.getBossBarOverlayValue(overlay));
        }

        @Override
        public int id() {
            return 4;
        }
    }

    public record UpdateFlagsAction(byte flags) implements Action {
        public UpdateFlagsAction(BossBar bar) {
            this(AdventurePacketConvertor.getBossBarFlagValue(bar.flags()));
        }

        public UpdateFlagsAction(NetworkBuffer reader) {
            this(reader.read(BYTE));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(BYTE, flags);
        }

        @Override
        public int id() {
            return 5;
        }
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.BOSS_BAR;
    }
}
