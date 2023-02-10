package net.minestom.server.event.player;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.ItemEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player is finished eating.
 */
public class PlayerEatEvent implements ItemEvent, PlayerInstanceEvent {

    private final Player player;
    private final ItemStack foodItem;
    private final Player.Hand hand;

    public PlayerEatEvent(Player player, ItemStack foodItem, Player.Hand hand) {
        this.player = player;
        this.foodItem = foodItem;
        this.hand = hand;
    }

    /**
     * Gets the food item that has been eaten.
     *
     * @return the food item
     * @deprecated use getItemStack() for the eaten item
     */
    @Deprecated
    public ItemStack getFoodItem() {
        return foodItem;
    }

    public Player.Hand getHand() {
        return hand;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the food item that has been eaten.
     *
     * @return the food item
     */
    @Override
    public ItemStack getItemStack() { return foodItem; }
}
