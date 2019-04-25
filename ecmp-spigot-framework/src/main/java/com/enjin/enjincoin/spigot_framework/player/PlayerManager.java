package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.event.MinecraftPlayerQuitEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager implements Listener {

    private BasePlugin plugin;

    private Map<UUID, MinecraftPlayer> players = new ConcurrentHashMap<>();

    public PlayerManager(BasePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        MinecraftPlayer minecraftPlayer = new MinecraftPlayer(this.plugin, event.getPlayer());

        addPlayer(minecraftPlayer);
        // Fetch or create a User and Identity associated with the joining Player
        PlayerInitializationTask.create(this.plugin, minecraftPlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        MinecraftPlayer player = removePlayer(event.getPlayer());

        if (player != null) {
            Bukkit.getPluginManager().callEvent(new MinecraftPlayerQuitEvent(player));
            player.cleanUp();
        }
    }

    public Map<UUID, MinecraftPlayer> getPlayers() {
        return new HashMap<>(this.players);
    }

    public MinecraftPlayer getPlayer(UUID uuid) {
        if (uuid == null) throw new NullPointerException("uuid must not be null");
        return this.players.get(uuid);
    }

    public MinecraftPlayer getPlayer(String ethAddr) {
        if (ethAddr == null) throw new NullPointerException("ethAddr must not be null");
        return this.players.values().stream()
                .filter(player -> player.getIdentityData() != null
                        && ethAddr.equalsIgnoreCase(player.getIdentityData().getEthereumAddress()))
                .findFirst().orElse(null);
    }

    public MinecraftPlayer getPlayer(BigInteger identityId) {
        if (identityId == null) throw new NullPointerException("identityId must not be null");
        return this.players.values().stream()
                .filter(player -> player.getIdentityData() != null
                        && identityId.equals(player.getIdentityData().getId()))
                .findFirst().orElse(null);
    }

    public void addPlayer(MinecraftPlayer minecraftPlayer) {
        if (!minecraftPlayer.getBukkitPlayer().isOnline()) {
            return;
        }

        this.players.put(minecraftPlayer.getBukkitPlayer().getUniqueId(), minecraftPlayer);
    }

    public MinecraftPlayer removePlayer(Player bukkitPlayer) {
        return this.players.remove(bukkitPlayer.getUniqueId());
    }

}
