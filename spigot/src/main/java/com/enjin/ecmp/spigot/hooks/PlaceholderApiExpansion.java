package com.enjin.ecmp.spigot.hooks;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

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
        try {
            if (player == null) {
                return "";
            }

            EnjPlayer enjPlayer = bootstrap.getPlayerManager().getPlayer(player).orElse(null);

            if (identifier.equals(ENJ_BALANCE)) {
                return enjPlayer.getEnjBalance() == null ? "0" : enjPlayer.getEnjBalance()
                        .setScale(2, BigDecimal.ROUND_HALF_DOWN)
                        .toString();
            }

            if (identifier.equals(ETH_BALANCE)) {
                return enjPlayer.getEthBalance() == null ? "0" : enjPlayer.getEthBalance()
                        .setScale(2, BigDecimal.ROUND_HALF_DOWN)
                        .toString();
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
        } catch (Exception ex) {
            bootstrap.log(ex);
        }

        return null;
    }
}
