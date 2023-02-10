package net.minestom.server.recipe;

import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.DeclareRecipesPacket;
import org.jetbrains.annotations.NotNull;

public abstract class StonecutterRecipe extends Recipe {
    private String group;
    private DeclareRecipesPacket.Ingredient ingredient;
    private ItemStack result;

    protected StonecutterRecipe(
            String recipeId,
            String group,
            DeclareRecipesPacket.Ingredient ingredient,
            ItemStack result
    ) {
        super(Type.STONECUTTING, recipeId);
        this.group = group;
        this.ingredient = ingredient;
        this.result = result;
    }

    @NotNull
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @NotNull
    public DeclareRecipesPacket.Ingredient getIngredient() {
        return ingredient;
    }

    public void setIngredient(DeclareRecipesPacket.Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    @NotNull
    public ItemStack getResult() {
        return result;
    }

    public void setResult(ItemStack result) {
        this.result = result;
    }
}
