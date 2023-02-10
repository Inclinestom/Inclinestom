package net.minestom.server.command.builder.arguments.minecraft;

import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

public class ArgumentResourceLocation extends Argument<String> {

    public static final int SPACE_ERROR = 1;

    public ArgumentResourceLocation(String id) {
        super(id);
    }

    @NotNull
    @Override
    public String parse(String input) throws ArgumentSyntaxException {
        if (input.contains(StringUtils.SPACE))
            throw new ArgumentSyntaxException("Resource location cannot contain space character", input, SPACE_ERROR);

        return input;
    }

    @Override
    public String parser() {
        return "minecraft:resource_location";
    }

    @Override
    public String toString() {
        return String.format("ResourceLocation<%s>", getId());
    }
}
