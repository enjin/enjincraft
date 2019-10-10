package com.enjin.enjincraft.spigot.player;

import org.bukkit.entity.Player;

public class UnregisteredPlayerException extends RuntimeException {

    public UnregisteredPlayerException(Player player) {
        super(String.format("%s is not registered in the player manager", player.getName()));
    }

}
