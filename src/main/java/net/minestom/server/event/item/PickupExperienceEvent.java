package net.minestom.server.event.item;

import net.minestom.server.entity.ExperienceOrb;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNull;

public class PickupExperienceEvent implements CancellableEvent, PlayerInstanceEvent {

    private final Player player;
    private final ExperienceOrb experienceOrb;
    private short experienceCount;

    private boolean cancelled;

    public PickupExperienceEvent(Player player, ExperienceOrb experienceOrb) {
        this.player = player;
        this.experienceOrb = experienceOrb;
        this.experienceCount = experienceOrb.getExperienceCount();
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public ExperienceOrb getExperienceOrb() {
        return experienceOrb;
    }

    public short getExperienceCount() {
        return experienceCount;
    }

    public void setExperienceCount(short experienceCount) {
        this.experienceCount = experienceCount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
