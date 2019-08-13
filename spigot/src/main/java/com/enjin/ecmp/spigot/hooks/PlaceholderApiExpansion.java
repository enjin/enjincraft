package com.enjin.ecmp.spigot.hooks;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.ECPlayer;
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

    private SpigotBootstrap bootstrap;

    public PlaceholderApiExpansion(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public String getIdentifier() {
        return bootstrap.plugin().getDescription().getName();
    }

    @Override
    public String getAuthor() {
        return bootstrap.plugin().getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return bootstrap.plugin().getDescription().getVersion();
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

        ECPlayer ecPlayer = bootstrap.getPlayerManager().getPlayer(player);

        if (identifier.equals(ENJ_BALANCE)) {
            return ecPlayer.getEnjAllowance() == null ? "0" : ecPlayer.getEnjAllowance().toString();
        }

        if (identifier.equals(ETH_BALANCE)) {
            return ecPlayer.getEthBalance() == null ? "0" : ecPlayer.getEthBalance().toString();
        }

        if (identifier.equals(LINK_STATUS)) {
            return ecPlayer.isIdentityLoaded()
                    ? (ecPlayer.isLinked() ? LINKED : ecPlayer.getLinkingCode())
                    : LOADING;
        }

        if (identifier.equals(ENJ_URL)) {
            return URL;
        }

        if (identifier.equals(ETH_ADDR)) {
            return ecPlayer.isIdentityLoaded()
                    ? (ecPlayer.isLinked() ? ecPlayer.getEthereumAddress() : NOT_AVAILABLE)
                    : LOADING;
        }

        return null;
    }
}
