package com.enjin.enjincraft.spigot.player;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PlayerManagerApi {

    Map<UUID, EnjPlayer> getPlayers();

    Optional<EnjPlayer> getPlayer(Player player);

    Optional<EnjPlayer> getPlayer(UUID uuid);

    Optional<EnjPlayer> getPlayer(String ethAddr);

    Optional<EnjPlayer> getPlayer(Integer identityId);

}
