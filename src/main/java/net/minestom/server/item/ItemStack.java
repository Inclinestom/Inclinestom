package net.minestom.server.item;

import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagWritable;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.*;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;

/**
 * Represents an immutable item to be placed inside {@link net.minestom.server.inventory.PlayerInventory},
 * {@link net.minestom.server.inventory.Inventory} or even on the ground {@link net.minestom.server.entity.ItemEntity}.
 * <p>
 * An item stack cannot be null, {@link ItemStack#AIR} should be used instead.
 */
public sealed interface ItemStack extends TagReadable, HoverEventSource<HoverEvent.ShowItem>
        permits ItemStackImpl {
    /**
     * Constant AIR item. Should be used instead of 'null'.
     */
    ItemStack AIR = ItemStack.of(Material.AIR);

    @Contract(value = "_ -> new", pure = true)
    static Builder builder(Material material) {
        return new ItemStackImpl.Builder(material, 1);
    }

    @Contract(value = "_ ,_ -> new", pure = true)
    static ItemStack of(Material material, int amount) {
        return ItemStackImpl.create(material, amount);
    }

    @Contract(value = "_ -> new", pure = true)
    static ItemStack of(Material material) {
        return of(material, 1);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    static ItemStack fromNBT(Material material, @Nullable NBTCompound nbtCompound, int amount) {
        if (nbtCompound == null) return of(material, amount);
        return builder(material).amount(amount).meta(nbtCompound).build();
    }

    @Contract(value = "_, _ -> new", pure = true)
    static ItemStack fromNBT(Material material, @Nullable NBTCompound nbtCompound) {
        return fromNBT(material, nbtCompound, 1);
    }

    /**
     * Converts this item to an NBT tag containing the id (material), count (amount), and tag (meta).
     *
     * @param nbtCompound The nbt representation of the item
     */
    @ApiStatus.Experimental
    static ItemStack fromItemNBT(NBTCompound nbtCompound) {
        String id = nbtCompound.getString("id");
        Check.notNull(id, "Item NBT must contain an id field.");
        Material material = Material.fromNamespaceId(id);
        Check.notNull(material, "Unknown material: {0}", id);

        Byte amount = nbtCompound.getByte("Count");
        if (amount == null) amount = 1;
        final NBTCompound tag = nbtCompound.getCompound("tag");
        return tag != null ? fromNBT(material, tag, amount) : of(material, amount);
    }

    @Contract(pure = true)
    Material material();

    @Contract(pure = true)
    int amount();

    @Contract(pure = true)
    ItemMeta meta();

    @Contract(pure = true)
    @ApiStatus.Experimental
    <T extends ItemMetaView<?>> T meta(Class<T> metaClass);

    @Contract(value = "_, -> new", pure = true)
    ItemStack with(Consumer<Builder> consumer);

    @Contract(value = "_, _ -> new", pure = true)
    @ApiStatus.Experimental
    <V extends ItemMetaView.Builder, T extends ItemMetaView<V>> ItemStack withMeta(Class<T> metaType,
                                                                                            Consumer<V> consumer);

    @Contract(value = "_ -> new", pure = true)
    ItemStack withMeta(Consumer<ItemMeta.Builder> consumer);

    @Contract(value = "_, -> new", pure = true)
    ItemStack withMaterial(Material material);

    @Contract(value = "_, -> new", pure = true)
    ItemStack withAmount(int amount);

    @Contract(value = "_, -> new", pure = true)
    default ItemStack withAmount(IntUnaryOperator intUnaryOperator) {
        return withAmount(intUnaryOperator.applyAsInt(amount()));
    }

    @ApiStatus.Experimental
    @Contract(value = "_, -> new", pure = true)
    ItemStack consume(int amount);

    @Contract(pure = true)
    default @Nullable Component getDisplayName() {
        return meta().getDisplayName();
    }

    @Contract(pure = true)
    default List<Component> getLore() {
        return meta().getLore();
    }

    @ApiStatus.Experimental
    @Contract(value = "_ -> new", pure = true)
    ItemStack withMeta(ItemMeta meta);

    @Contract(value = "_, -> new", pure = true)
    default ItemStack withDisplayName(@Nullable Component displayName) {
        return withMeta(builder -> builder.displayName(displayName));
    }

    @Contract(value = "_, -> new", pure = true)
    default ItemStack withDisplayName(UnaryOperator<@Nullable Component> componentUnaryOperator) {
        return withDisplayName(componentUnaryOperator.apply(getDisplayName()));
    }

    @Contract(value = "_, -> new", pure = true)
    default ItemStack withLore(List<? extends Component> lore) {
        return withMeta(builder -> builder.lore(lore));
    }

    @Contract(value = "_, -> new", pure = true)
    default ItemStack withLore(UnaryOperator<List<Component>> loreUnaryOperator) {
        return withLore(loreUnaryOperator.apply(getLore()));
    }

    @Contract(pure = true)
    default boolean isAir() {
        return material() == Material.AIR;
    }

    @Contract(pure = true)
    boolean isSimilar(ItemStack itemStack);

    @Contract(value = "_, _ -> new", pure = true)
    default <T> ItemStack withTag(Tag<T> tag, @Nullable T value) {
        return withMeta(builder -> builder.set(tag, value));
    }

    @Override
    default <T> @UnknownNullability T getTag(Tag<T> tag) {
        return meta().getTag(tag);
    }

    @Override
    default HoverEvent<HoverEvent.ShowItem> asHoverEvent(UnaryOperator<HoverEvent.ShowItem> op) {
        final BinaryTagHolder tagHolder = BinaryTagHolder.encode(meta().toNBT(), MinestomAdventure.NBT_CODEC);
        return HoverEvent.showItem(op.apply(HoverEvent.ShowItem.of(material(), amount(), tagHolder)));
    }

    /**
     * Converts this item to an NBT tag containing the id (material), count (amount), and tag (meta)
     *
     * @return The nbt representation of the item
     */
    @ApiStatus.Experimental
    NBTCompound toItemNBT();


    @Deprecated
    @Contract(pure = true)
    default Material getMaterial() {
        return material();
    }

    @Deprecated
    @Contract(pure = true)
    default int getAmount() {
        return amount();
    }

    @Deprecated
    @Contract(pure = true)
    default ItemMeta getMeta() {
        return meta();
    }

    sealed interface Builder extends TagWritable
            permits ItemStackImpl.Builder {
        @Contract(value = "_ -> this")
        Builder amount(int amount);

        @Contract(value = "_ -> this")
        Builder meta(TagHandler tagHandler);

        @Contract(value = "_ -> this")
        Builder meta(NBTCompound compound);

        @Contract(value = "_ -> this")
        Builder meta(ItemMeta itemMeta);

        @Contract(value = "_ -> this")
        Builder meta(Consumer<ItemMeta.Builder> consumer);

        @Contract(value = "_, _ -> this")
        <V extends ItemMetaView.Builder, T extends ItemMetaView<V>> Builder meta(Class<T> metaType,
                                                                                          Consumer<V> itemMetaConsumer);

        @Contract(value = "-> new", pure = true)
        ItemStack build();

        @Contract(value = "_, _ -> this")
        default <T> Builder set(Tag<T> tag, @Nullable T value) {
            setTag(tag, value);
            return this;
        }

        @Contract(value = "_ -> this")
        default Builder displayName(@Nullable Component displayName) {
            return meta(builder -> builder.displayName(displayName));
        }

        @Contract(value = "_ -> this")
        default Builder lore(List<? extends Component> lore) {
            return meta(builder -> builder.lore(lore));
        }

        @Contract(value = "_ -> this")
        default Builder lore(Component... lore) {
            return meta(builder -> builder.lore(lore));
        }
    }
}
