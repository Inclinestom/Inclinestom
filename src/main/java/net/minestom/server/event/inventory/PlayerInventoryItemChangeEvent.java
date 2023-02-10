package net.minestom.server.event.inventory;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called when {@link AbstractInventory#safeItemInsert(int, ItemStack)} is being invoked on a {@link PlayerInventory}.
 * This event cannot be cancelled and items related to the change are already moved.
 * <p>
 * When this event is being called, {@link InventoryItemChangeEvent} listeners will also be triggered, so you can
 * listen only for an ancestor event and check whether it is an instance of that class.
 */
@SuppressWarnings("JavadocReference")
public class PlayerInventoryItemChangeEvent extends InventoryItemChangeEvent implements PlayerInstanceEvent {

    private final Player player;

    public PlayerInventoryItemChangeEvent(Player player, int slot, ItemStack previousItem, ItemStack newItem) {
        super(null, slot, previousItem, newItem);
        this.player = player;
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}
