package net.minestom.server.command.builder.arguments.minecraft;

import com.google.gson.JsonParseException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import org.jetbrains.annotations.NotNull;

public class ArgumentComponent extends Argument<Component> {

    public static final int INVALID_JSON_ERROR = 1;

    public ArgumentComponent(String id) {
        super(id, true);
    }

    @NotNull
    @Override
    public Component parse(String input) throws ArgumentSyntaxException {
        try {
            return GsonComponentSerializer.gson().deserialize(input);
        } catch (JsonParseException e) {
            throw new ArgumentSyntaxException("Invalid JSON", input, INVALID_JSON_ERROR);
        }
    }

    @Override
    public String parser() {
        return "minecraft:component";
    }

    @Override
    public String toString() {
        return String.format("Component<%s>", getId());
    }
}
