package com.enjin.ecmp.spigot.events;

import com.enjin.ecmp.spigot.player.EnjPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EnjinCoinPlayerQuitEvent extends Event {

    private static HandlerList handlers = new HandlerList();

    private final EnjPlayer player;

    public EnjinCoinPlayerQuitEvent(EnjPlayer player) {
        this.player = player;
    }

    public EnjPlayer getPlayer() {
        return this.player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
