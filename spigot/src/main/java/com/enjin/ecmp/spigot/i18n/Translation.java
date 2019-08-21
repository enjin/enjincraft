package com.enjin.ecmp.spigot.i18n;

import com.enjin.ecmp.spigot.configuration.ConfigurationException;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.util.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public enum Translation {

    _locale("en_US"),
    _version(1),

    COMMAND_ROOT_DESCRIPTION("Show information about the plugin and authors."),

    COMMAND_BALANCE_DESCRIPTION("Show wallet address, eth, enj, and token balances."),
    COMMAND_BALANCE_WALLETADDRESS("&6Wallet Address: &d%s"),
    COMMAND_BALANCE_IDENTITYID("&6Identity ID: &d%s"),
    COMMAND_BALANCE_ENJBALANCE("&a[ %s ENJ ]"),
    COMMAND_BALANCE_ETHBALANCE("&a[ %s ETH ]"),
    COMMAND_BALANCE_TOKENDISPLAY("&6%s. &5%s &a(qty. %s)"),
    COMMAND_BALANCE_NOTOKENS("&l&6No tokens found in your wallet."),
    COMMAND_BALANCE_TOKENCOUNT("&l&6Found %s tokens in your wallet."),

    COMMAND_HELP_DESCRIPTION("Show list of commands with their usage."),

    COMMAND_LINK_DESCRIPTION("Show linking code or linked address."),

    COMMAND_SEND_DESCRIPTION("Send the held token to another player."),

    COMMAND_TRADE_DESCRIPTION("Show help for trade sub-commands."),
    COMMAND_TRADE_INVITE_DESCRIPTION("Invite a player to trade tokens."),
    COMMAND_TRADE_ACCEPT_DESCRIPTION("Accept another player's trade invite."),
    COMMAND_TRADE_DECLINE_DESCRIPTION("Decline another player's trade invite."),

    COMMAND_UNLINK_DESCRIPTION("Removes link between wallet and player account."),

    COMMAND_WALLET_DESCRIPTION("Open your wallet inventory to view, check out, and return items.");

    public static final String DEFAULT_LOCALE = "en_US";

    private static YamlConfiguration LANG;

    private String path;
    private Object def;

    Translation(String path, Object def) {
        this.path = path;
        this.def = def;
    }

    Translation(Object def) {
        this.path = this.name().replace('_', '.');
        if (this.path.startsWith("."))
            this.path = "internal" + path;
        this.def = def;
    }

    public String path() {
        return path;
    }

    public String defaultTranslation() {
        return String.valueOf(def);
    }

    public String translation() {
        return LANG.getString(path(), defaultTranslation());
    }

    public int version() {
        return Integer.parseInt(_version.translation());
    }

    public String locale() {
        return _locale.translation();
    }

    @Override
    public String toString() {
        return TextUtil.colorize(translation());
    }

    public void sendMessage(CommandSender sender, Object... args) {
        MessageUtils.sendString(sender, String.format(translation(), args));
    }

    public static void setLang(YamlConfiguration config) {
        LANG = config;
        setDefaults();
    }

    private static void setDefaults() {
        if (LANG == null) return;

        for (Translation translation : values()) {
            if (!LANG.isSet(translation.path))
                LANG.set(translation.path, translation.def);
        }
    }

}
