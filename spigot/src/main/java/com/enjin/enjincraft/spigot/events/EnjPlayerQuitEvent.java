package com.enjin.enjincraft.spigot.events;

import com.enjin.enjincraft.spigot.player.EnjPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EnjPlayerQuitEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final EnjPlayer player;

    public EnjPlayerQuitEvent(EnjPlayer player) {
        this.player = player;
    }

    public EnjPlayer getPlayer() {
        return this.player;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
