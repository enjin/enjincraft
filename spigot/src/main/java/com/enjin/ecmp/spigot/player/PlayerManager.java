package com.enjin.ecmp.spigot.player;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.events.EnjinCoinPlayerQuitEvent;
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
    private Map<UUID, ECPlayer> players = new ConcurrentHashMap<>();

    public PlayerManager(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        ECPlayer ecPlayer = new ECPlayer(bootstrap, event.getPlayer());
        addPlayer(ecPlayer);
        // Fetch or create a User and Identity associated with the joining Player
        PlayerInitializationTask.create(bootstrap, ecPlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ECPlayer player = removePlayer(event.getPlayer());

        if (player != null) {
            Bukkit.getPluginManager().callEvent(new EnjinCoinPlayerQuitEvent(player));
            player.cleanUp();
        }
    }

    public Map<UUID, ECPlayer> getPlayers() {
        return new HashMap<>(this.players);
    }

    public ECPlayer getPlayer(UUID uuid) {
        if (uuid == null) throw new NullPointerException("uuid must not be null");
        return this.players.get(uuid);
    }

    public ECPlayer getPlayer(String ethAddr) {
        if (ethAddr == null) throw new NullPointerException("ethAddr must not be null");
        return this.players.values().stream()
                .filter(player -> player.getEthereumAddress() != null
                        && ethAddr.equalsIgnoreCase(player.getEthereumAddress()))
                .findFirst().orElse(null);
    }

    public ECPlayer getPlayer(Integer identityId) {
        if (identityId == null) throw new NullPointerException("identityId must not be null");
        return this.players.values().stream()
                .filter(player -> player.getIdentityId() != null
                        && identityId.equals(player.getIdentityId()))
                .findFirst().orElse(null);
    }

    public void addPlayer(ECPlayer player) {
        if (!player.getBukkitPlayer().isOnline()) return;
        this.players.put(player.getBukkitPlayer().getUniqueId(), player);
    }

    public ECPlayer removePlayer(Player bukkitPlayer) {
        return this.players.remove(bukkitPlayer.getUniqueId());
    }

}
