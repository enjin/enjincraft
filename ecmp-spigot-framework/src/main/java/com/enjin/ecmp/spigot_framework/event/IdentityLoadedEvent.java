package com.enjin.ecmp.spigot_framework.event;

import com.enjin.ecmp.spigot_framework.player.EnjinCoinPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class IdentityLoadedEvent extends Event {

    private static HandlerList handlers = new HandlerList();

    private final EnjinCoinPlayer player;

    public IdentityLoadedEvent(EnjinCoinPlayer player) {
        this.player = player;
    }

    public EnjinCoinPlayer getPlayer() {
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
