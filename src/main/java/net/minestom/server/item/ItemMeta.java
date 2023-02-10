package net.minestom.server.item;

import net.kyori.adventure.text.Component;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.attribute.ItemAttribute;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.ProtocolObject;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.Taggable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.*;
import java.util.function.Consumer;

public sealed interface ItemMeta extends TagReadable, NetworkBuffer.Writer
        permits ItemMetaImpl {

    @Override
    <T> @UnknownNullability T getTag(Tag<T> tag);

    @Contract(value = "_, -> new", pure = true)
    ItemMeta with(Consumer<Builder> builderConsumer);

    NBTCompound toNBT();

    String toSNBT();

    @Contract(pure = true)
    default int getDamage() {
        return getTag(ItemTags.DAMAGE);
    }

    @Contract(pure = true)
    default boolean isUnbreakable() {
        return getTag(ItemTags.UNBREAKABLE);
    }

    @Contract(pure = true)
    default int getHideFlag() {
        return getTag(ItemTags.HIDE_FLAGS);
    }

    @Contract(pure = true)
    default @Nullable Component getDisplayName() {
        return getTag(ItemTags.NAME);
    }

    @Contract(pure = true)
    default List<Component> getLore() {
        return getTag(ItemTags.LORE);
    }

    @Contract(pure = true)
    default Map<Enchantment, Short> getEnchantmentMap() {
        return getTag(ItemTags.ENCHANTMENTS);
    }

    @Contract(pure = true)
    default List<ItemAttribute> getAttributes() {
        return getTag(ItemTags.ATTRIBUTES);
    }

    @Contract(pure = true)
    default int getCustomModelData() {
        return getTag(ItemTags.CUSTOM_MODEL_DATA);
    }

    @Contract(pure = true)
    default Set<String> getCanDestroy() {
        return Set.copyOf(getTag(ItemTags.CAN_DESTROY));
    }

    @Contract(pure = true)
    default boolean canDestroy(Block block) {
        return getCanDestroy().contains(block.name());
    }

    @Contract(pure = true)
    default Set<String> getCanPlaceOn() {
        return Set.copyOf(getTag(ItemTags.CAN_PLACE_ON));
    }

    @Contract(pure = true)
    default boolean canPlaceOn(Block block) {
        return getCanPlaceOn().contains(block.name());
    }

    sealed interface Builder extends Taggable
            permits ItemMetaImpl.Builder, ItemMetaView.Builder {
        ItemMeta build();

        default <T> Builder set(Tag<T> tag, @Nullable T value) {
            setTag(tag, value);
            return this;
        }

        @Contract("_ -> this")
        default Builder damage(int damage) {
            return set(ItemTags.DAMAGE, damage);
        }

        @Contract("_ -> this")
        default Builder unbreakable(boolean unbreakable) {
            return set(ItemTags.UNBREAKABLE, unbreakable);
        }

        @Contract("_ -> this")
        default Builder hideFlag(int hideFlag) {
            return set(ItemTags.HIDE_FLAGS, hideFlag);
        }

        @Contract("_ -> this")
        default Builder hideFlag(ItemHideFlag... hideFlags) {
            int result = 0;
            for (ItemHideFlag hideFlag : hideFlags) result |= hideFlag.getBitFieldPart();
            return hideFlag(result);
        }

        @Contract("_ -> this")
        default Builder displayName(@Nullable Component displayName) {
            return set(ItemTags.NAME, displayName);
        }

        @Contract("_ -> this")
        default Builder lore(List<? extends Component> lore) {
            return set(ItemTags.LORE, lore.isEmpty() ? null : List.class.cast(lore));
        }

        @Contract("_ -> this")
        default Builder lore(Component... lore) {
            return lore(Arrays.asList(lore));
        }

        @Contract("_ -> this")
        default Builder enchantments(Map<Enchantment, Short> enchantments) {
            return set(ItemTags.ENCHANTMENTS, Map.copyOf(enchantments));
        }

        @Contract("_, _ -> this")
        default Builder enchantment(Enchantment enchantment, short level) {
            var enchantments = new HashMap<>(getTag(ItemTags.ENCHANTMENTS));
            enchantments.put(enchantment, level);
            return enchantments(enchantments);
        }

        @Contract("-> this")
        default Builder clearEnchantment() {
            return enchantments(Map.of());
        }

        @Contract("_ -> this")
        default Builder attributes(List<ItemAttribute> attributes) {
            return set(ItemTags.ATTRIBUTES, attributes.isEmpty() ? null : attributes);
        }

        @Contract("_ -> this")
        default Builder customModelData(int customModelData) {
            return set(ItemTags.CUSTOM_MODEL_DATA, customModelData);
        }

        @Contract("_ -> this")
        default Builder canPlaceOn(Set<Block> blocks) {
            return set(ItemTags.CAN_PLACE_ON, blocks.stream().map(ProtocolObject::name).toList());
        }

        @Contract("_ -> this")
        default Builder canPlaceOn(Block... blocks) {
            return canPlaceOn(Set.of(blocks));
        }

        @Contract("_ -> this")
        default Builder canDestroy(Set<Block> blocks) {
            return set(ItemTags.CAN_DESTROY, blocks.stream().map(ProtocolObject::name).toList());
        }

        @Contract("_ -> this")
        default Builder canDestroy(Block... blocks) {
            return canDestroy(Set.of(blocks));
        }
    }
}
