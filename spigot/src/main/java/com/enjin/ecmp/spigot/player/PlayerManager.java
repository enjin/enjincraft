package com.enjin.ecmp.spigot.player;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.events.EnjPlayerQuitEvent;
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

public class PlayerManager implements Listener {

    private SpigotBootstrap bootstrap;
    private Map<UUID, EnjPlayer> players = new ConcurrentHashMap<>();

    public PlayerManager(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        EnjPlayer enjPlayer = new EnjPlayer(bootstrap, event.getPlayer());
        addPlayer(enjPlayer);
        // Fetch or create a User and Identity associated with the joining Player
        PlayerInitializationTask.create(bootstrap, enjPlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        EnjPlayer player = removePlayer(event.getPlayer());
        if (player == null) return;
        Bukkit.getPluginManager().callEvent(new EnjPlayerQuitEvent(player));
        player.cleanUp();
    }

    public Map<UUID, EnjPlayer> getPlayers() {
        return new HashMap<>(this.players);
    }

    public EnjPlayer getPlayer(Player player) {
        if (player == null) throw new NullPointerException("player must not be null");
        return getPlayer(player.getUniqueId());
    }

    public EnjPlayer getPlayer(UUID uuid) {
        if (uuid == null) throw new NullPointerException("uuid must not be null");
        return this.players.get(uuid);
    }

    public EnjPlayer getPlayer(String ethAddr) {
        if (ethAddr == null) throw new NullPointerException("ethAddr must not be null");
        return this.players.values().stream()
                .filter(player -> player.getEthereumAddress() != null
                        && ethAddr.equalsIgnoreCase(player.getEthereumAddress()))
                .findFirst().orElse(null);
    }

    public EnjPlayer getPlayer(Integer identityId) {
        if (identityId == null) throw new NullPointerException("identityId must not be null");
        return this.players.values().stream()
                .filter(player -> player.getIdentityId() != null
                        && identityId.equals(player.getIdentityId()))
                .findFirst().orElse(null);
    }

    public void addPlayer(EnjPlayer player) {
        if (!player.getBukkitPlayer().isOnline()) return;
        this.players.put(player.getBukkitPlayer().getUniqueId(), player);
    }

    public EnjPlayer removePlayer(Player bukkitPlayer) {
        return this.players.remove(bukkitPlayer.getUniqueId());
    }

}
