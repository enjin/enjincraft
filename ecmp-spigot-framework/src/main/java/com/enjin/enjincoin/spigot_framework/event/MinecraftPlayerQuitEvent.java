package com.enjin.enjincoin.spigot_framework.event;

import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MinecraftPlayerQuitEvent extends Event {

    private static HandlerList handlers = new HandlerList();

    private final MinecraftPlayer player;

    public MinecraftPlayerQuitEvent(MinecraftPlayer player) {
        this.player = player;
    }

    public MinecraftPlayer getPlayer() {
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
