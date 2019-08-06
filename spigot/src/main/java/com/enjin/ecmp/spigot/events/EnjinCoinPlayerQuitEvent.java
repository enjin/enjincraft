package com.enjin.ecmp.spigot.events;

import com.enjin.ecmp.spigot.player.ECPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EnjinCoinPlayerQuitEvent extends Event {

    private static HandlerList handlers = new HandlerList();

    private final ECPlayer player;

    public EnjinCoinPlayerQuitEvent(ECPlayer player) {
        this.player = player;
    }

    public ECPlayer getPlayer() {
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
