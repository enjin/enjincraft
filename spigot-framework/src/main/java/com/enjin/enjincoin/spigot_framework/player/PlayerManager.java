package com.enjin.enjincoin.spigot_framework.player;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
        removePlayer(event.getPlayer());
    }

    public Map<UUID, MinecraftPlayer> getPlayers() {
        // Return shallow copy of the players map
        return new HashMap<>(this.players);
    }

    public MinecraftPlayer getPlayer(UUID playerUuid) {
        return this.players.get(playerUuid);
    }

    public void addPlayer(MinecraftPlayer minecraftPlayer) {
        if (!minecraftPlayer.getBukkitPlayer().isOnline()) {
            return;
        }

        this.players.put(minecraftPlayer.getBukkitPlayer().getUniqueId(), minecraftPlayer);
    }

    public void removePlayer(Player bukkitPlayer) {
        MinecraftPlayer user = this.players.remove(bukkitPlayer.getUniqueId());
        if (user != null) {
            user.cleanUp();
        }

        PlayerInitializationTask.cleanUp(bukkitPlayer.getUniqueId());
    }

}
