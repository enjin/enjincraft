package com.enjin.enjincraft.spigot.player;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public interface PlayerManager {

    Map<UUID, EnjPlayer> getPlayers();

    EnjPlayer getPlayer(Player player);

    EnjPlayer getPlayer(UUID uuid);

    EnjPlayer getPlayer(String ethAddr);

    EnjPlayer getPlayer(Integer identityId);

}
