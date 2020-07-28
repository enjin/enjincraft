package com.enjin.enjincraft.spigot.player;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.events.EnjPlayerQuitEvent;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManagerImpl implements Listener, PlayerManager {

    private final SpigotBootstrap bootstrap;
    private final Map<UUID, EnjPlayer> players = new ConcurrentHashMap<>();

    public PlayerManagerImpl(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            EnjPlayer enjPlayer = new EnjPlayer(bootstrap, event.getPlayer());
            addPlayer(enjPlayer);
            // Fetch or create a User and Identity associated with the joining Player
            PlayerInitializationTask.create(bootstrap, enjPlayer);
            enjPlayer.removeQrMap();
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            EnjPlayer player = removePlayer(event.getPlayer());
            if (player == null)
                return;

            Bukkit.getPluginManager().callEvent(new EnjPlayerQuitEvent(player));
            player.cleanUp();
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    @Override
    public Map<UUID, EnjPlayer> getPlayers() {
        return new HashMap<>(players);
    }

    @Override
    public EnjPlayer getPlayer(@NonNull Player player) throws NullPointerException {
        return getPlayer(player.getUniqueId());
    }

    @Override
    public EnjPlayer getPlayer(@NonNull UUID uuid) throws NullPointerException {
        return players.get(uuid);
    }

    @Override
    public EnjPlayer getPlayer(@NonNull String ethAddr) throws NullPointerException {
        return players.values()
                .stream()
                .filter(player -> player.getEthereumAddress() != null && ethAddr.equalsIgnoreCase(player.getEthereumAddress()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public EnjPlayer getPlayer(@NonNull Integer identityId) throws NullPointerException {
        return players.values()
                .stream()
                .filter(player -> player.getIdentityId() != null && identityId.equals(player.getIdentityId()))
                .findFirst()
                .orElse(null);
    }

    public void addPlayer(@NonNull EnjPlayer player) throws IllegalArgumentException, NullPointerException {
        if (!player.getBukkitPlayer().isOnline())
            throw new IllegalArgumentException("Player must be online");

        players.put(player.getBukkitPlayer().getUniqueId(), player);
    }

    public EnjPlayer removePlayer(@NonNull Player player) {
        return players.remove(player.getUniqueId());
    }

}
