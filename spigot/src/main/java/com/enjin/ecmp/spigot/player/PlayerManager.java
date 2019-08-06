package com.enjin.ecmp.spigot.player;

import com.enjin.ecmp.spigot.EcmpPlugin;
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

    private EcmpPlugin plugin;
    private Map<UUID, EnjinCoinPlayer> players = new ConcurrentHashMap<>();

    public PlayerManager(EcmpPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        EnjinCoinPlayer enjinCoinPlayer = new EnjinCoinPlayer(this.plugin, event.getPlayer());

        addPlayer(enjinCoinPlayer);
        // Fetch or create a User and Identity associated with the joining Player
        PlayerInitializationTask.create(this.plugin, enjinCoinPlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        EnjinCoinPlayer player = removePlayer(event.getPlayer());

        if (player != null) {
            Bukkit.getPluginManager().callEvent(new EnjinCoinPlayerQuitEvent(player));
            player.cleanUp();
        }
    }

    public Map<UUID, EnjinCoinPlayer> getPlayers() {
        return new HashMap<>(this.players);
    }

    public EnjinCoinPlayer getPlayer(UUID uuid) {
        if (uuid == null) throw new NullPointerException("uuid must not be null");
        return this.players.get(uuid);
    }

    public EnjinCoinPlayer getPlayer(String ethAddr) {
        if (ethAddr == null) throw new NullPointerException("ethAddr must not be null");
        return this.players.values().stream()
                .filter(player -> player.getEthereumAddress() != null
                        && ethAddr.equalsIgnoreCase(player.getEthereumAddress()))
                .findFirst().orElse(null);
    }

    public EnjinCoinPlayer getPlayer(Integer identityId) {
        if (identityId == null) throw new NullPointerException("identityId must not be null");
        return this.players.values().stream()
                .filter(player -> player.getIdentityId() != null
                        && identityId.equals(player.getIdentityId()))
                .findFirst().orElse(null);
    }

    public void addPlayer(EnjinCoinPlayer enjinCoinPlayer) {
        if (!enjinCoinPlayer.getBukkitPlayer().isOnline()) return;
        this.players.put(enjinCoinPlayer.getBukkitPlayer().getUniqueId(), enjinCoinPlayer);
    }

    public EnjinCoinPlayer removePlayer(Player bukkitPlayer) {
        return this.players.remove(bukkitPlayer.getUniqueId());
    }

}
