package net.minestom.server.item.attribute;

import net.minestom.server.attribute.Attribute;
import net.minestom.server.attribute.AttributeOperation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record ItemAttribute(UUID uuid,
                            String name,
                            Attribute attribute,
                            AttributeOperation operation, double amount,
                            AttributeSlot slot) {
}
