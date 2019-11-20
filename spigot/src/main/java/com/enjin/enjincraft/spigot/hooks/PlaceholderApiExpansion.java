package com.enjin.enjincraft.spigot.hooks;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Optional;

public class PlaceholderApiExpansion extends PlaceholderExpansion {

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
        try {
            if (player == null)
                return "";

            Optional<EnjPlayer> enjPlayerOptional = bootstrap.getPlayerManager().getPlayer(player);
            if (!enjPlayerOptional.isPresent())
                return "";
            EnjPlayer enjPlayer = enjPlayerOptional.get();

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
                if (enjPlayer.isIdentityLoaded()) {
                    if (enjPlayer.isLinked())
                        return LINKED;
                    else
                        return enjPlayer.getLinkingCode();
                } else {
                    return LOADING;
                }
            }

            if (identifier.equals(ENJ_URL))
                return URL;

            if (identifier.equals(ETH_ADDR)) {
                if (enjPlayer.isIdentityLoaded()) {
                    if (enjPlayer.isLinked())
                        return enjPlayer.getEthereumAddress();
                    else
                        return NOT_AVAILABLE;
                } else {
                    return LOADING;
                }
            }
        } catch (Exception ex) {
            bootstrap.log(ex);
        }

        return null;
    }
}
