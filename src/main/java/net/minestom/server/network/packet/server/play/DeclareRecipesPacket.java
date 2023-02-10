package net.minestom.server.network.packet.server.play;

import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.minestom.server.network.NetworkBuffer.*;

public record DeclareRecipesPacket(List<DeclaredRecipe> recipes) implements ServerPacket {
    public DeclareRecipesPacket {
        recipes = List.copyOf(recipes);
    }

    public DeclareRecipesPacket(NetworkBuffer reader) {
        this(reader.readCollection(r -> {
            final String type = r.read(STRING);
            return switch (type) {
                case "crafting_shapeless" -> new DeclaredShapelessCraftingRecipe(reader);
                case "crafting_shaped" -> new DeclaredShapedCraftingRecipe(reader);
                case "smelting" -> new DeclaredSmeltingRecipe(reader);
                case "blasting" -> new DeclaredBlastingRecipe(reader);
                case "smoking" -> new DeclaredSmokingRecipe(reader);
                case "campfire_cooking" -> new DeclaredCampfireCookingRecipe(reader);
                case "stonecutter" -> new DeclaredStonecutterRecipe(reader);
                case "smithing" -> new DeclaredSmithingRecipe(reader);
                default -> throw new UnsupportedOperationException("Unrecognized type: " + type);
            };
        }));
    }

    @Override
    public void write(NetworkBuffer writer) {
        writer.writeCollection(recipes, (bWriter, recipe) -> {
            bWriter.write(STRING, recipe.type());
            bWriter.write(STRING, recipe.recipeId());
            bWriter.write(recipe);
        });
    }

    @Override
    public int getId() {
        return ServerPacketIdentifier.DECLARE_RECIPES;
    }

    public sealed interface DeclaredRecipe extends NetworkBuffer.Writer
            permits DeclaredShapelessCraftingRecipe, DeclaredShapedCraftingRecipe,
            DeclaredSmeltingRecipe, DeclaredBlastingRecipe, DeclaredSmokingRecipe,
            DeclaredCampfireCookingRecipe, DeclaredStonecutterRecipe, DeclaredSmithingRecipe {
        String type();

        String recipeId();
    }

    public record DeclaredShapelessCraftingRecipe(String recipeId, String group,
                                                  List<Ingredient> ingredients,
                                                  ItemStack result) implements DeclaredRecipe {
        private DeclaredShapelessCraftingRecipe(NetworkBuffer reader) {
            this(reader.read(STRING), reader.read(STRING),
                    reader.readCollection(Ingredient::new), reader.read(ITEM));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(STRING, group);
            writer.writeCollection(ingredients);
            writer.write(ITEM, result);
        }

        @Override
        public String type() {
            return "crafting_shapeless";
        }
    }

    public record DeclaredShapedCraftingRecipe(String recipeId, int width, int height,
                                               String group, List<Ingredient> ingredients,
                                               ItemStack result) implements DeclaredRecipe {
        public DeclaredShapedCraftingRecipe {
            ingredients = List.copyOf(ingredients);
        }

        private DeclaredShapedCraftingRecipe(DeclaredShapedCraftingRecipe packet) {
            this(packet.recipeId, packet.width, packet.height, packet.group, packet.ingredients, packet.result);
        }

        public DeclaredShapedCraftingRecipe(NetworkBuffer reader) {
            this(read(reader));
        }

        private static DeclaredShapedCraftingRecipe read(NetworkBuffer reader) {

            String recipeId = reader.read(STRING);
            int width = reader.read(VAR_INT);
            int height = reader.read(VAR_INT);
            String group = reader.read(STRING);
            List<Ingredient> ingredients = new ArrayList<>();
            for (int slot = 0; slot < width * height; slot++) {
                ingredients.add(new Ingredient(reader));
            }
            ItemStack result = reader.read(ITEM);
            return new DeclaredShapedCraftingRecipe(recipeId, width, height, group, ingredients, result);
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(VAR_INT, width);
            writer.write(VAR_INT, height);
            writer.write(STRING, group);
            for (Ingredient ingredient : ingredients) {
                ingredient.write(writer);
            }
            writer.write(ITEM, result);
        }

        @Override
        public String type() {
            return "crafting_shaped";
        }
    }

    public record DeclaredSmeltingRecipe(String recipeId, String group,
                                         Ingredient ingredient, ItemStack result,
                                         float experience, int cookingTime) implements DeclaredRecipe {
        public DeclaredSmeltingRecipe(NetworkBuffer reader) {
            this(reader.read(STRING), reader.read(STRING),
                    new Ingredient(reader), reader.read(ITEM),
                    reader.read(FLOAT), reader.read(VAR_INT));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(STRING, group);
            writer.write(ingredient);
            writer.write(ITEM, result);
            writer.write(FLOAT, experience);
            writer.write(VAR_INT, cookingTime);
        }

        @Override
        public String type() {
            return "smelting";
        }
    }

    public record DeclaredBlastingRecipe(String recipeId, String group,
                                         Ingredient ingredient, ItemStack result,
                                         float experience, int cookingTime) implements DeclaredRecipe {
        public DeclaredBlastingRecipe(NetworkBuffer reader) {
            this(reader.read(STRING), reader.read(STRING),
                    new Ingredient(reader), reader.read(ITEM),
                    reader.read(FLOAT), reader.read(VAR_INT));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(STRING, group);
            writer.write(ingredient);
            writer.write(ITEM, result);
            writer.write(FLOAT, experience);
            writer.write(VAR_INT, cookingTime);
        }

        @Override
        public String type() {
            return "blasting";
        }
    }

    public record DeclaredSmokingRecipe(String recipeId, String group,
                                        Ingredient ingredient, ItemStack result,
                                        float experience, int cookingTime) implements DeclaredRecipe {
        public DeclaredSmokingRecipe(NetworkBuffer reader) {
            this(reader.read(STRING), reader.read(STRING),
                    new Ingredient(reader), reader.read(ITEM),
                    reader.read(FLOAT), reader.read(VAR_INT));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(STRING, group);
            writer.write(ingredient);
            writer.write(ITEM, result);
            writer.write(FLOAT, experience);
            writer.write(VAR_INT, cookingTime);
        }

        @Override
        public String type() {
            return "smoking";
        }
    }

    public record DeclaredCampfireCookingRecipe(String recipeId, String group,
                                                Ingredient ingredient, ItemStack result,
                                                float experience, int cookingTime) implements DeclaredRecipe {
        public DeclaredCampfireCookingRecipe(NetworkBuffer reader) {
            this(reader.read(STRING), reader.read(STRING),
                    new Ingredient(reader), reader.read(ITEM),
                    reader.read(FLOAT), reader.read(VAR_INT));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(STRING, group);
            writer.write(ingredient);
            writer.write(ITEM, result);
            writer.write(FLOAT, experience);
            writer.write(VAR_INT, cookingTime);
        }

        @Override
        public String type() {
            return "campfire_cooking";
        }
    }

    public record DeclaredStonecutterRecipe(String recipeId, String group,
                                            Ingredient ingredient, ItemStack result) implements DeclaredRecipe {
        public DeclaredStonecutterRecipe(NetworkBuffer reader) {
            this(reader.read(STRING), reader.read(STRING),
                    new Ingredient(reader), reader.read(ITEM));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(STRING, group);
            writer.write(ingredient);
            writer.write(ITEM, result);
        }

        @Override
        public String type() {
            return "stonecutter";
        }
    }

    public record DeclaredSmithingRecipe(String recipeId, Ingredient base, Ingredient addition,
                                         ItemStack result) implements DeclaredRecipe {
        public DeclaredSmithingRecipe(NetworkBuffer reader) {
            this(reader.read(STRING), new Ingredient(reader), new Ingredient(reader), reader.read(ITEM));
        }

        @Override
        public void write(NetworkBuffer writer) {
            writer.write(base);
            writer.write(addition);
            writer.write(ITEM, result);
        }

        @Override
        public String type() {
            return "smithing";
        }
    }

    public record Ingredient(List<ItemStack> items) implements NetworkBuffer.Writer {
        public Ingredient {
            items = List.copyOf(items);
        }

        public Ingredient(NetworkBuffer reader) {
            this(reader.readCollection(ITEM));
        }

        public void write(NetworkBuffer writer) {
            writer.writeCollection(ITEM, items);
        }
    }
}
