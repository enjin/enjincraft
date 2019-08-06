package com.enjin.ecmp.spigot.hooks;

import com.enjin.ecmp.spigot.EcmpPlugin;
import com.enjin.ecmp.spigot.EcmpSpigot;
import com.enjin.ecmp.spigot.player.EnjinCoinPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderApiExpansion extends PlaceholderExpansion {

    public static final String ENJ_BALANCE = "enj_balance";
    public static final String ETH_BALANCE = "eth_balance";
    public static final String LINK_STATUS = "link_status";
    public static final String ENJ_URL = "enj_url";
    public static final String ETH_ADDR = "eth_addr";

    public static final String LOADING = "loading";
    public static final String LINKED = "linked";
    public static final String NOT_AVAILABLE = "n/a";
    public static final String URL = "www.enjincoin.io";

    private EcmpPlugin plugin;

    public PlaceholderApiExpansion(EcmpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return plugin.getDescription().getName();
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        EnjinCoinPlayer enjPlayer = EcmpSpigot.bootstrap().getPlayerManager().getPlayer(player.getUniqueId());

        if (identifier.equals(ENJ_BALANCE)) {
            return enjPlayer.getEnjAllowance() == null ? "0" : enjPlayer.getEnjAllowance().toString();
        }

        if (identifier.equals(ETH_BALANCE)) {
            return enjPlayer.getEthBalance() == null ? "0" : enjPlayer.getEthBalance().toString();
        }

        if (identifier.equals(LINK_STATUS)) {
            return enjPlayer.isIdentityLoaded()
                    ? (enjPlayer.isLinked() ? LINKED : enjPlayer.getLinkingCode())
                    : LOADING;
        }

        if (identifier.equals(ENJ_URL)) {
            return URL;
        }

        if (identifier.equals(ETH_ADDR)) {
            return enjPlayer.isIdentityLoaded()
                    ? (enjPlayer.isLinked() ? enjPlayer.getEthereumAddress() : NOT_AVAILABLE)
                    : LOADING;
        }

        return null;
    }
}
