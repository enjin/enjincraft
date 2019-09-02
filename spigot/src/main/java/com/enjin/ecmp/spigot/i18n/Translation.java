package com.enjin.ecmp.spigot.i18n;

import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.util.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Translation {

    _locale("en_US"),
    _version(1),

    COMMAND_API_BADUSAGE("&cInvalid command usage!"),
    COMMAND_API_USAGE("USAGE: %s"),
    COMMAND_API_REQUIREMENTS_INVALIDPLAYER("&cThis command cannot be used by players."),
    COMMAND_API_REQUIREMENTS_INVALIDCONSOLE("&cThis command cannot be used by the console."),
    COMMAND_API_REQUIREMENTS_INVALIDREMOTE("&cThis command cannot be used remotely."),
    COMMAND_API_REQUIREMENTS_INVALIDBLOCK("&cThis command cannot be used by command blocks."),
    COMMAND_API_REQUIREMENTS_NOPERMISSION("&cYou do not have the permission required for this command: &6%s"),

    COMMAND_ROOT_DESCRIPTION("Show information about the plugin and authors."),
    COMMAND_ROOT_DETAILS("&7Running &6%s &cv%s&7.<br>&7Use &6%s &7to view available commands."),

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
    COMMAND_LINK_NULLWALLET("&cUnable to show your wallet address."),
    COMMAND_LINK_SHOWWALLET("&aYour account is linked to the wallet address: &6%s"),
    COMMAND_LINK_NULLCODE("&cUnable to show your identity linking code."),
    COMMAND_LINK_INSTRUCTIONS_1("&6To link your account follow the steps below:"),
    COMMAND_LINK_INSTRUCTIONS_2("&7Download the Enjin Wallet app for Android or iOS"),
    COMMAND_LINK_INSTRUCTIONS_3("&7Browse to the Linked Apps section"),
    COMMAND_LINK_INSTRUCTIONS_4("&7Click the 'LINK APP' button"),
    COMMAND_LINK_INSTRUCTIONS_5("&7Select the wallet you wish to link"),
    COMMAND_LINK_INSTRUCTIONS_6("&7Enter the identity linking code shown below"),
    COMMAND_LINK_INSTRUCTIONS_7("&aIdentity Code: &6%s"),

    COMMAND_SEND_DESCRIPTION("Send the held token to another player."),
    COMMAND_SEND_SUBMITTED("&aSend request submitted successfully. Please confirm the request in your wallet app."),
    COMMAND_SEND_MUSTHOLDITEM("&cYou must hold the tokenized item you wish to send."),
    COMMAND_SEND_ITEMNOTTOKEN("&cThe held item is not associated with a token."),

    COMMAND_CONF_DESCRIPTION("Perform operations on the plugin configuration."),
    COMMAND_CONF_SET_DESCRIPTION("Set new value for config option."),
    COMMAND_CONF_SET_LANG_DESCRIPTION("Set the default language to use."),

    COMMAND_TRADE_DESCRIPTION("Show help for trade sub-commands."),
    COMMAND_TRADE_INVITE_DESCRIPTION("Invite a player to trade tokens."),
    COMMAND_TRADE_ACCEPT_DESCRIPTION("Accept another player's trade invite."),
    COMMAND_TRADE_DECLINE_DESCRIPTION("Decline another player's trade invite."),
    COMMAND_TRADE_NOOPENINVITE("&cNo open trade invite from &6%s"),
    COMMAND_TRADE_DECLINED_SENDER("&aYou have declined &6%s's &atrade invite."),
    COMMAND_TRADE_DECLINED_TARGET("&6%s &chas declined your trade invite."),
    COMMAND_TRADE_ALREADYINVITED("&cYou have already invited &6%s &cto trade."),
    COMMAND_TRADE_WANTSTOTRADE("&6%s &awants to trade with you."),
    COMMAND_TRADE_INVITESENT("&aTrade invite sent to &6%s!"),
    COMMAND_TRADE_INVITEDTOTRADE("&6%s &7has invited you to trade."),
    COMMAND_TRADE_CONFIRM_ACTION("&6Please confirm the trade in your wallet app."),
    COMMAND_TRADE_CONFIRM_WAIT("&6Please wait while the other player confirms the trade."),
    COMMAND_TRADE_COMPLETE("&6Your trade is complete!"),

    COMMAND_UNLINK_DESCRIPTION("Removes link between wallet and player account."),
    COMMAND_UNLINK_SUCCESS("&aThe wallet has been unlinked from your account!"),

    COMMAND_WALLET_DESCRIPTION("Open your wallet inventory to view, check out, and return items."),

    HINT_LINK("&cType &6'/enj link' &cfor instructions on how to link your Enjin Wallet."),

    IDENTITY_NOTLOADED("&cYour player data is loading. Try again momentarily."),

    ERRORS_EXCEPTION("&cERROR: &7%s"),
    ERRORS_CHOOSEOTHERPLAYER("&cYou must specify a player other than yourself."),
    ERRORS_PLAYERNOTONLINE("&6%s &cis not online."),

    MISC_NEWLINE(""),

    WALLET_NOTLINKED_SELF("&cYou have not linked a wallet to your identity."),
    WALLET_NOTLINKED_OTHER("&6%s &chas not linked a wallet."),
    WALLET_ALLOWANCENOTSET("&cYou must approve the allowance request in your wallet before you can send or trade tokens.");

    private static final Logger LOGGER = Logger.getLogger("ECMP");
    public static final String DEFAULT_LOCALE = "en_US";

    private static YamlConfiguration LANG;

    private String path;
    private Object def;
    private int argCount;

    Translation(Object def) {
        this.path = this.name().replace('_', '.');
        if (this.path.startsWith("."))
            this.path = "internal" + path;
        this.def = def;
        this.argCount = getArgCount(String.valueOf(def));
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

    public void send(CommandSender sender, Object... args) {
        String formatted = String.format(translation(), args);
        String[] lines = formatted.split("<br>");
        for (String line : lines)
            MessageUtils.sendString(sender, line);
    }

    public static void setLang(YamlConfiguration config) {
        LANG = config;
        setDefaults();
    }

    private static void setDefaults() {
        if (LANG == null) return;

        for (Translation translation : values()) {
            if (!LANG.isSet(translation.path)) {
                LANG.set(translation.path, translation.def);
                LOGGER.info(String.format("Setting missing translation key %s to default English translation.",
                        translation.path));
            } else {
                int argCount = getArgCount(LANG.getString(translation.path));
                if (argCount != translation.argCount) {
                    LANG.set(translation.path, translation.def);
                    LOGGER.info(String.format("Invalid translation key %s, using default English translation.",
                            translation.path));
                }
            }
        }
    }

    private static int getArgCount(String text) {
        int argCount = 0;
        Matcher matcher = Pattern.compile("%s").matcher(text);
        while (matcher.find())
            argCount += 1;
        return argCount;
    }

}
