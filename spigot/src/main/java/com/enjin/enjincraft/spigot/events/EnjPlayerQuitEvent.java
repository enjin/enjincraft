package com.enjin.enjincraft.spigot.events;

import com.enjin.enjincraft.spigot.player.EnjPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EnjPlayerQuitEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final EnjPlayer player;

    public EnjPlayerQuitEvent(EnjPlayer player) {
        this.player = player;
    }

    public EnjPlayer getPlayer() {
        return this.player;
    }

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
