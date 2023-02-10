package net.minestom.server.item;

import net.minestom.server.item.rule.VanillaStackingRule;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTByte;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTString;

import java.util.Map;
import java.util.function.Consumer;

record ItemStackImpl(Material material, int amount, ItemMetaImpl meta) implements ItemStack {
    static final StackingRule DEFAULT_STACKING_RULE;

    static {
        final String stackingRuleProperty = System.getProperty("minestom.stacking-rule");
        if (stackingRuleProperty == null) {
            DEFAULT_STACKING_RULE = new VanillaStackingRule();
        } else {
            try {
                DEFAULT_STACKING_RULE = (StackingRule) ClassLoader.getSystemClassLoader()
                        .loadClass(stackingRuleProperty).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Could not instantiate default stacking rule", e);
            }
        }
    }

    static ItemStack create(Material material, int amount, ItemMetaImpl meta) {
        if (amount <= 0) return AIR;
        return new ItemStackImpl(material, amount, meta);
    }

    static ItemStack create(Material material, int amount) {
        return create(material, amount, ItemMetaImpl.EMPTY);
    }

    @Override
    public <T extends ItemMetaView<?>> T meta(Class<T> metaClass) {
        return ItemMetaViewImpl.construct(metaClass, meta);
    }

    @Override
    public ItemStack with(Consumer<ItemStack.Builder> consumer) {
        ItemStack.Builder builder = builder();
        consumer.accept(builder);
        return builder.build();
    }

    @Override
    public <V extends ItemMetaView.Builder, T extends ItemMetaView<V>> ItemStack withMeta(Class<T> metaType,
                                                                                                   Consumer<V> consumer) {
        return builder().meta(metaType, consumer).build();
    }

    @Override
    public ItemStack withMeta(Consumer<ItemMeta.Builder> consumer) {
        return builder().meta(consumer).build();
    }

    @Override
    public ItemStack withMaterial(Material material) {
        return new ItemStackImpl(material, amount, meta);
    }

    @Override
    public ItemStack withAmount(int amount) {
        return create(material, amount, meta);
    }

    @Override
    public ItemStack consume(int amount) {
        return DEFAULT_STACKING_RULE.apply(this, currentAmount -> currentAmount - amount);
    }

    @Override
    public ItemStack withMeta(ItemMeta meta) {
        return new ItemStackImpl(material, amount, (ItemMetaImpl) meta);
    }

    @Override
    public boolean isSimilar(ItemStack itemStack) {
        return material == itemStack.material() && meta.equals(itemStack.meta());
    }

    @Override
    public NBTCompound toItemNBT() {
        final NBTString material = NBT.String(material().name());
        final NBTByte amount = NBT.Byte(amount());
        final NBTCompound nbt = meta().toNBT();
        if (nbt.isEmpty()) return NBT.Compound(Map.of("id", material, "Count", amount));
        return NBT.Compound(Map.of("id", material, "Count", amount, "tag", nbt));
    }

    @Contract(value = "-> new", pure = true)
    private ItemStack.Builder builder() {
        return new Builder(material, amount, new ItemMetaImpl.Builder(meta.tagHandler().copy()));
    }

    static final class Builder implements ItemStack.Builder {
        final Material material;
        int amount;
        ItemMetaImpl.Builder metaBuilder;

        Builder(Material material, int amount, ItemMetaImpl.Builder metaBuilder) {
            this.material = material;
            this.amount = amount;
            this.metaBuilder = metaBuilder;
        }

        Builder(Material material, int amount) {
            this(material, amount, new ItemMetaImpl.Builder(TagHandler.newHandler()));
        }

        @Override
        public ItemStack.Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        @Override
        public ItemStack.Builder meta(TagHandler tagHandler) {
            return metaBuilder(new ItemMetaImpl.Builder(tagHandler.copy()));
        }

        @Override
        public ItemStack.Builder meta(NBTCompound compound) {
            return metaBuilder(new ItemMetaImpl.Builder(TagHandler.fromCompound(compound)));
        }

        @Override
        public ItemStack.Builder meta(ItemMeta itemMeta) {
            final TagHandler tagHandler = ((ItemMetaImpl) itemMeta).tagHandler();
            return metaBuilder(new ItemMetaImpl.Builder(tagHandler.copy()));
        }

        @Override
        public ItemStack.Builder meta(Consumer<ItemMeta.Builder> consumer) {
            consumer.accept(metaBuilder);
            return this;
        }

        @Override
        public <V extends ItemMetaView.Builder, T extends ItemMetaView<V>> ItemStack.Builder meta(Class<T> metaType,
                                                                                                           Consumer<V> itemMetaConsumer) {
            V view = ItemMetaViewImpl.constructBuilder(metaType, metaBuilder.tagHandler());
            itemMetaConsumer.accept(view);
            return this;
        }

        @Override
        public <T> void setTag(Tag<T> tag, @Nullable T value) {
            this.metaBuilder.setTag(tag, value);
        }

        @Override
        public ItemStack build() {
            return ItemStackImpl.create(material, amount, metaBuilder.build());
        }

        private ItemStack.Builder metaBuilder(ItemMetaImpl.Builder builder) {
            this.metaBuilder = builder;
            return this;
        }
    }
}
