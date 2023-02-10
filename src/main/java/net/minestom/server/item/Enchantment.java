package net.minestom.server.item;

import net.minestom.server.registry.ProtocolObject;
import net.minestom.server.registry.Registry;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public sealed interface Enchantment extends ProtocolObject, Enchantments permits EnchantmentImpl {

    /**
     * Returns the enchantment registry.
     *
     * @return the enchantment registry
     */
    @Contract(pure = true)
    Registry.EnchantmentEntry registry();

    @Override
    default NamespaceID namespace() {
        return registry().namespace();
    }

    @Override
    default int id() {
        return registry().id();
    }

    static Collection<Enchantment> values() {
        return EnchantmentImpl.values();
    }

    static @Nullable Enchantment fromNamespaceId(String namespaceID) {
        return EnchantmentImpl.getSafe(namespaceID);
    }

    static @Nullable Enchantment fromNamespaceId(NamespaceID namespaceID) {
        return fromNamespaceId(namespaceID.asString());
    }

    static @Nullable Enchantment fromId(int id) {
        return EnchantmentImpl.getId(id);
    }
}
