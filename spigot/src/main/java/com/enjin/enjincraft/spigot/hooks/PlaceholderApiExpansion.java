package com.enjin.enjincraft.spigot.hooks;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.math.RoundingMode;
import java.util.Optional;

public class PlaceholderApiExpansion extends PlaceholderExpansion {

    private static final String EMPTY = "";

    public static final String ENJ_BALANCE = "enj_balance";
    public static final String ETH_BALANCE = "eth_balance";
    public static final String LINK_STATUS = "link_status";
    public static final String ENJ_URL = "enj_url";
    public static final String ETH_ADDR = "eth_addr";

    public static final String LOADING = "loading";
    public static final String LINKED = "linked";
    public static final String NOT_AVAILABLE = "n/a";
    public static final String URL = "www.enjin.io";

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
        Optional<EnjPlayer> enjPlayerOptional = bootstrap.getPlayerManager().getPlayer(player);
        if (!enjPlayerOptional.isPresent())
            return EMPTY;

        try {
            return process(enjPlayerOptional.get(), identifier);
        } catch (Exception ex) {
            bootstrap.log(ex);
        }

        return EMPTY;
    }

    public String process(EnjPlayer player, String identifier) {
        String result = EMPTY;

        switch (identifier) {
            case ENJ_BALANCE:
                result = getEnjBalance(player);
                break;
            case ETH_BALANCE:
                result = getEthBalance(player);
                break;
            case LINK_STATUS:
                result = getLinkStatus(player);
                break;
            case ENJ_URL:
                result = URL;
                break;
            case ETH_ADDR:
                result = getEthAddress(player);
                break;
            default:
                break;
        }

        return result;
    }

    private String getEnjBalance(EnjPlayer player) {
        return player.getEnjBalance() == null ? "0" : player.getEnjBalance()
                .setScale(2, RoundingMode.HALF_DOWN)
                .toString();
    }

    private String getEthBalance(EnjPlayer player) {
        return player.getEthBalance() == null ? "0" : player.getEthBalance()
                .setScale(2, RoundingMode.HALF_DOWN)
                .toString();
    }

    private String getLinkStatus(EnjPlayer player) {
        if (player.isIdentityLoaded()) {
            if (player.isLinked())
                return LINKED;
            else
                return player.getLinkingCode();
        }

        return LOADING;
    }

    private String getEthAddress(EnjPlayer player) {
        if (player.isIdentityLoaded()) {
            if (player.isLinked())
                return player.getEthereumAddress();
            else
                return NOT_AVAILABLE;
        }

        return LOADING;
    }
}
