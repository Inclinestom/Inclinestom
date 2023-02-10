package net.minestom.server.network.packet.server.play;

import net.kyori.adventure.text.Component;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.adventure.ComponentHolder;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ComponentHoldingServerPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import static net.minestom.server.network.NetworkBuffer.*;

public record AdvancementsPacket(boolean reset, List<AdvancementMapping> advancementMappings,
                                 List<String> identifiersToRemove,
                                 List<ProgressMapping> progressMappings) implements ComponentHoldingServerPacket {
    public AdvancementsPacket {
        advancementMappings = List.copyOf(advancementMappings);
        identifiersToRemove = List.copyOf(identifiersToRemove);
        progressMappings = List.copyOf(progressMappings);
    }

    public AdvancementsPacket(NetworkBuffer reader) {
        this(reader.read(BOOLEAN), reader.readCollection(AdvancementMapping::new),
                reader.readCollection(STRING),
                reader.readCollection(ProgressMapping::new));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.write(BOOLEAN, reset);
        writer.writeCollection(advancementMappings);
        writer.writeCollection(STRING, identifiersToRemove);
        writer.writeCollection(progressMappings);
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.ADVANCEMENTS;
    }

    // TODO is the display-item needed to be updated?
    @Override
    public Collection<Component> components() {
        final var displayData = this.advancementMappings.stream().map(AdvancementMapping::value).map(Advancement::displayData).filter(Objects::nonNull).toList();
        final var titles = displayData.stream().map(DisplayData::title).toList();
        final var descriptions = displayData.stream().map(DisplayData::description).toList();

        final var list = new ArrayList<Component>();

        list.addAll(titles);
        list.addAll(descriptions);

        return List.copyOf(list);
    }

    @Override
    public ServerPacket copyWithOperator(final UnaryOperator<Component> operator) {
        return new AdvancementsPacket(
                this.reset,
                this.advancementMappings.stream().map(mapping -> mapping.copyWithOperator(operator)).toList(),
                this.identifiersToRemove,
                this.progressMappings
        );
    }

    /**
     * AdvancementMapping maps the namespaced ID to the Advancement.
     */
    public record AdvancementMapping(String key,
                                     Advancement value) implements NetworkBuffer.Writer, ComponentHolder<AdvancementMapping> {
        public AdvancementMapping(NetworkBuffer reader) {
            this(reader.read(STRING), new Advancement(reader));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(STRING, key);
            writer.write(value);
        }

        @Override
        public Collection<Component> components() {
            return this.value.components();
        }

        @Override
        public AdvancementMapping copyWithOperator(UnaryOperator<Component> operator) {
            return this.value.displayData == null ? this : new AdvancementMapping(this.key, this.value.copyWithOperator(operator));
        }
    }

    public record Advancement(@Nullable String parentIdentifier, @Nullable DisplayData displayData,
                              List<String> criteria,
                              List<Requirement> requirements) implements NetworkBuffer.Writer, ComponentHolder<Advancement> {
        public Advancement {
            criteria = List.copyOf(criteria);
            requirements = List.copyOf(requirements);
        }

        public Advancement(NetworkBuffer reader) {
            this(reader.read(BOOLEAN) ? reader.read(STRING) : null,
                    reader.read(BOOLEAN) ? new DisplayData(reader) : null,
                    reader.readCollection(STRING),
                    reader.readCollection(Requirement::new));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.writeOptional(STRING, parentIdentifier);
            writer.writeOptional(displayData);
            writer.writeCollection(STRING, criteria);
            writer.writeCollection(requirements);
        }

        @Override
        public Collection<Component> components() {
            return this.displayData != null ? this.displayData.components() : List.of();
        }

        @Override
        public Advancement copyWithOperator(UnaryOperator<Component> operator) {
            return this.displayData == null ? this : new Advancement(this.parentIdentifier, this.displayData.copyWithOperator(operator), this.criteria, this.requirements);
        }
    }

    public record Requirement(List<String> requirements) implements NetworkBuffer.Writer {
        public Requirement {
            requirements = List.copyOf(requirements);
        }

        public Requirement(NetworkBuffer reader) {
            this(reader.readCollection(STRING));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.writeCollection(STRING, requirements);
        }
    }

    public record DisplayData(Component title, Component description,
                              ItemStack icon, FrameType frameType,
                              int flags, @Nullable String backgroundTexture,
                              float x, float y) implements NetworkBuffer.Writer, ComponentHolder<DisplayData> {
        public DisplayData(NetworkBuffer reader) {
            this(read(reader));
        }

        private DisplayData(DisplayData displayData) {
            this(displayData.title, displayData.description,
                    displayData.icon, displayData.frameType,
                    displayData.flags, displayData.backgroundTexture,
                    displayData.x, displayData.y);
        }

        private static DisplayData read(NetworkBuffer reader) {
            var title = reader.read(COMPONENT);
            var description = reader.read(COMPONENT);
            var icon = reader.read(ITEM);
            var frameType = FrameType.values()[reader.read(VAR_INT)];
            var flags = reader.read(INT);
            var backgroundTexture = (flags & 0x1) != 0 ? reader.read(STRING) : null;
            var x = reader.read(FLOAT);
            var y = reader.read(FLOAT);
            return new DisplayData(title, description,
                    icon, frameType,
                    flags, backgroundTexture,
                    x, y);
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(COMPONENT, title);
            writer.write(COMPONENT, description);
            writer.write(ITEM, icon);
            writer.write(VAR_INT, frameType.ordinal());
            writer.write(INT, flags);
            if ((flags & 0x1) != 0) {
                assert backgroundTexture != null;
                writer.write(STRING, backgroundTexture);
            }
            writer.write(FLOAT, x);
            writer.write(FLOAT, y);
        }

        @Override
        public Collection<Component> components() {
            return List.of(this.title, this.description);
        }

        @Override
        public DisplayData copyWithOperator(UnaryOperator<Component> operator) {
            return new DisplayData(operator.apply(this.title), operator.apply(this.description), this.icon, this.frameType, this.flags, this.backgroundTexture, this.x, this.y);
        }
    }

    public record ProgressMapping(String key,
                                  AdvancementProgress progress) implements NetworkBuffer.Writer {
        public ProgressMapping(NetworkBuffer reader) {
            this(reader.read(STRING), new AdvancementProgress(reader));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(STRING, key);
            writer.write(progress);
        }
    }

    public record AdvancementProgress(List<Criteria> criteria) implements NetworkBuffer.Writer {
        public AdvancementProgress {
            criteria = List.copyOf(criteria);
        }

        public AdvancementProgress(NetworkBuffer reader) {
            this(reader.readCollection(Criteria::new));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.writeCollection(criteria);
        }
    }

    public record Criteria(String criterionIdentifier,
                           CriterionProgress criterionProgress) implements NetworkBuffer.Writer {
        public Criteria(NetworkBuffer reader) {
            this(reader.read(STRING), new CriterionProgress(reader));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(STRING, criterionIdentifier);
            writer.write(criterionProgress);
        }
    }

    public record CriterionProgress(@Nullable Long dateOfAchieving) implements NetworkBuffer.Writer {
        public CriterionProgress(NetworkBuffer reader) {
            this(reader.readOptional(LONG));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.writeOptional(LONG, dateOfAchieving);
        }
    }
}