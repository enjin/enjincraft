package com.enjin.enjincraft.spigot.i18n;

import com.enjin.enjincraft.spigot.Bootstrap;
import com.enjin.enjincraft.spigot.EnjinCraft;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.configuration.Conf;
import com.enjin.enjincraft.spigot.util.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Translation {

    _locale("en_US"),
    _language("English"),
    _version(1),

    COMMAND_API_BADUSAGE("&cInvalid command usage!"),
    COMMAND_API_USAGE("USAGE: %s"),
    COMMAND_API_REQUIREMENTS_INVALIDPLAYER("&cThis command cannot be used by players."),
    COMMAND_API_REQUIREMENTS_INVALIDCONSOLE("&cThis command cannot be used by the console."),
    COMMAND_API_REQUIREMENTS_INVALIDREMOTE("&cThis command cannot be used by RCON."),
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
    COMMAND_LINK_INSTRUCTIONS_7("&aLink Code: &6%s"),

    COMMAND_DEVSEND_DESCRIPTION("Send a token from the developer wallet to a player."),
    COMMAND_DEVSEND_INVALIDAMOUNT("&cA valid amount to send must be specified."),
    COMMAND_DEVSEND_INVALIDTOKEN("&cA valid id for the token must be specified."),

    COMMAND_SEND_DESCRIPTION("Send the held token to another player."),
    COMMAND_SEND_SUBMITTED("&aSend request submitted successfully. Please confirm the request in your wallet app."),
    COMMAND_SEND_MUSTHOLDITEM("&cYou must hold the tokenized item you wish to send."),
    COMMAND_SEND_ITEMNOTTOKEN("&cThe held item is not associated with a token."),
    COMMAND_SEND_DOESNOTHAVETOKEN("&cYou can not send a token you do not have."),

    COMMAND_CONF_DESCRIPTION("Perform operations on the plugin configuration."),
    COMMAND_CONF_SET_DESCRIPTION("Set new value for config option."),
    COMMAND_CONF_SET_LANG_DESCRIPTION("Set the default language to use."),
    COMMAND_CONF_SET_LANG_NOTFOUND("&cThat is not a valid language option."),
    COMMAND_CONF_SET_LANG_SUCCESS("&aYou have set the server language to &6%s!"),

    COMMAND_TOKEN_DESCRIPTION("Perform operations on token definitions."),
    COMMAND_TOKEN_CREATE_DESCRIPTION("Add a fungible token definition for the held item and provided id to the config."),
    COMMAND_TOKEN_CREATE_SUCCESS("&aToken created."),
    COMMAND_TOKEN_CREATE_FAILED("&cToken was not created."),
    COMMAND_TOKEN_CREATE_DUPLICATE("&cToken id already exists."),
    COMMAND_TOKEN_CREATENFT_DESCRIPTION("Add a non-fungible token definition for the held item provided id and index to the config."),
    COMMAND_TOKEN_CREATENFT_BASEFAILED("&cUnable to create the non-fungible base token."),
    COMMAND_TOKEN_CREATENFT_DUPLICATE("&cToken id and index already exists."),
    COMMAND_TOKEN_CREATENFT_REPLACENICKNAME("&cCannot replace token nickname on creation."),
    COMMAND_TOKEN_UPDATE_DESCRIPTION("Update a token's definition with the held item and the token's id."),
    COMMAND_TOKEN_UPDATE_SUCCESS("&aToken updated."),
    COMMAND_TOKEN_UPDATE_FAILED("&cToken was not updated."),
    COMMAND_TOKEN_DELETE_DESCRIPTION("Deletes a token's definition &cforever&f."),
    COMMAND_TOKEN_DELETE_SUCCESS("&aToken deleted."),
    COMMAND_TOKEN_DELETE_FAILED("&cToken was not deleted."),
    COMMAND_TOKEN_DELETE_BASENFT_1("&cNon-fungible base cannot be deleted while instances of it still exist."),
    COMMAND_TOKEN_DELETE_BASENFT_2("&cUse &6'/enj token list <id>' &cto see its instances."),
    COMMAND_TOKEN_TOINV_DESCRIPTION("Add a non-tokenized copy of the token to your inventory."),
    COMMAND_TOKEN_TOINV_SUCCESS("&aThe item was added to your inventory."),
    COMMAND_TOKEN_TOINV_FAILED("&cNo space in inventory to add the item to."),
    COMMAND_TOKEN_NICKNAME_DESCRIPTION("Add a nickname to a token for command usages."),
    COMMAND_TOKEN_NICKNAME_SUCCESS("&aToken nickname added."),
    COMMAND_TOKEN_NICKNAME_DUPLICATE("&cToken nickname already exists."),
    COMMAND_TOKEN_NICKNAME_HAS("&cThe token already has the nickname."),
    COMMAND_TOKEN_NICKNAME_INVALID("&cInvalid nickname."),
    COMMAND_TOKEN_ADDPERM_DESCRIPTION("Add a permission to a token by referencing the token's id."),
    COMMAND_TOKEN_ADDPERM_PERMADDED("&aPermission added to token."),
    COMMAND_TOKEN_ADDPERM_PERMREJECTED("&cPermission is blacklisted for tokens."),
    COMMAND_TOKEN_ADDPERM_DUPLICATEPERM("&cPermission already on token."),
    COMMAND_TOKEN_ADDPERMNFT_DESCRIPTION("Add a permission to a non-fungible token by referencing its id and index."),
    COMMAND_TOKEN_REVOKEPERM_DESCRIPTION("Remove a permission from a token by referencing the token's id."),
    COMMAND_TOKEN_REVOKEPERM_PERMREVOKED("&aPermission removed from token."),
    COMMAND_TOKEN_REVOKEPERM_PERMNOTONTOKEN("&cPermission not found on token."),
    COMMAND_TOKEN_REVOKEPERMNFT_DESCRIPTION("Remove a permission from a non-fungible token by referencing its id and index."),
    COMMAND_TOKEN_PERM_ISGLOBAL("&cPermission is global."),
    COMMAND_TOKEN_GETURI_DESCRIPTION("Retrieves the token's metadata URI from the platform and sets it for the token."),
    COMMAND_TOKEN_GETURI_SUCCESS("&aMetadata URI set for token."),
    COMMAND_TOKEN_GETURI_FAILED("&cMetadata URI was not set for the token."),
    COMMAND_TOKEN_GETURI_EMPTY_1("&cToken has no metadata URI set on the platform."),
    COMMAND_TOKEN_GETURI_EMPTY_2("&cUse &6'/enj token removeuri' &cto remove any metadata URI."),
    COMMAND_TOKEN_REMOVEURI_DESCRIPTION("Removes the token's metadata URI."),
    COMMAND_TOKEN_REMOVEURI_SUCCESS("&aMetadata URI removed from token."),
    COMMAND_TOKEN_REMOVEURI_FAILED("&cMetadata URI was not removed from the token."),
    COMMAND_TOKEN_REMOVEURI_EMPTY("&cToken has no metadata URI set."),
    COMMAND_TOKEN_SETWALLETVIEW_DESCRIPTION("Sets a token's view and retrieval state within the wallet."),
    COMMAND_TOKEN_SETWALLETVIEW_INVALIDVIEW("&cNot a valid view state."),
    COMMAND_TOKEN_SETWALLETVIEW_HAS("&cToken is already set to the view state."),
    COMMAND_TOKEN_LIST_DESCRIPTION("Lists the tokens specified."),
    COMMAND_TOKEN_LIST_EMPTY("&aFound no tokens."),
    COMMAND_TOKEN_LIST_HEADER_TOKENS("Token(s)"),
    COMMAND_TOKEN_LIST_HEADER_NONFUNGIBLE("Non-Fungible Token(s)"),
    COMMAND_TOKEN_EXPORT_DESCRIPTION("Exports token configurations to file."),
    COMMAND_TOKEN_EXPORT_COMPLETE("&aExport complete."),
    COMMAND_TOKEN_EXPORT_SUCCESS("&aExported all tokens."),
    COMMAND_TOKEN_EXPORT_EMPTY("&cThere are no tokens to export."),
    COMMAND_TOKEN_EXPORT_FAILED("&cCould not export token(s)."),
    COMMAND_TOKEN_EXPORT_PARTIAL("&cFailed to export one or more tokens."),
    COMMAND_TOKEN_IMPORT_DESCRIPTION("Imports token configurations from file."),
    COMMAND_TOKEN_IMPORT_COMPLETE("&aImport complete."),
    COMMAND_TOKEN_IMPORT_SUCCESS("&aImported all tokens."),
    COMMAND_TOKEN_IMPORT_EMPTY("&cThere are no tokens files to import."),
    COMMAND_TOKEN_IMPORT_FAILED("&cCould not import token(s)."),
    COMMAND_TOKEN_IMPORT_PARTIAL("&cFailed to import one or more tokens."),
    COMMAND_TOKEN_NOSUCHTOKEN("&cToken not found."),
    COMMAND_TOKEN_NOHELDITEM("&cMust hold an item to use as a token."),
    COMMAND_TOKEN_INVALIDFULLID("&cInvalid id and/or index for token."),
    COMMAND_TOKEN_INVALIDID("&cInvalid id for token."),
    COMMAND_TOKEN_INVALIDDATA("&cThe token's data is not valid."),
    COMMAND_TOKEN_ISFUNGIBLE("&cCommand cannot be used on fungible tokens."),
    COMMAND_TOKEN_ISNONFUNGIBLE("&cCommand cannot be used on non-fungible tokens."),
    COMMAND_TOKEN_ISNONFUNGIBLEINSTANCE("&cCommand cannot be used for individual non-fungible tokens."),
    COMMAND_TOKEN_MUSTPASSINDEX("&cMust provide an index for non-fungible tokens."),

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

    COMMAND_QR_DESCRIPTION("Receive a map with a QR code on it for you to link your wallet with."),
    COMMAND_QR_ALREADYLINKED("&cYou are already linked."),
    COMMAND_QR_INVENTORYFULL("&cYour inventory is full."),
    COMMAND_QR_CODENOTLOADED("&cThe QR code is not loaded yet."),
    COMMAND_QR_ERROR("&cAn error occurred while creating the map."),

    COMMAND_UNLINK_DESCRIPTION("Removes link between wallet and player account."),
    COMMAND_UNLINK_SUCCESS("&aThe wallet has been unlinked from your account!"),

    COMMAND_WALLET_DESCRIPTION("Open your wallet inventory to view, check out, and return items."),

    HINT_LINK("&cType &6'/enj link' &cfor instructions on how to link your Enjin Wallet."),

    IDENTITY_NOTLOADED("&cYour player data is loading. Try again momentarily."),

    ERRORS_EXCEPTION("&cERROR: &7%s"),
    ERRORS_CHOOSEOTHERPLAYER("&cYou must specify a player other than yourself."),
    ERRORS_INVALIDPLAYERNAME("&6%s &cis not a valid player name."),
    ERRORS_PLAYERNOTONLINE("&6%s &cis not online."),
    ERRORS_PLAYERNOTREGISTERED("&6%s &chas not been registered by the plugin yet."),

    MISC_NEWLINE(""),

    QR_DISPLAYNAME("Enjin QR Code"),

    WALLET_UI_FUNGIBLE("Go To Fungible"),
    WALLET_UI_NONFUNGIBLE("Go To Non-Fungible"),
    WALLET_NOTLINKED_SELF("&cYou have not linked a wallet to your identity."),
    WALLET_NOTLINKED_OTHER("&6%s &chas not linked a wallet."),
    WALLET_ALLOWANCENOTSET("&cYou must confirm the approve request in your wallet before you can send or trade tokens.");

    private static final Logger LOGGER = Logger.getLogger("EnjinCraft");
    public static final Locale DEFAULT_LOCALE = Locale.en_US;

    private static final Map<Locale, YamlConfiguration> LOCALE_CONFIGS = new EnumMap<>(Locale.class);
    private static final Map<Locale, String> LOCALE_NAMES = new EnumMap<>(Locale.class);
    private static Locale serverLocale = DEFAULT_LOCALE;

    private final String path;
    private final Object def;
    private final int argCount;

    Translation(Object def) {
        String path = name().replace('_', '.');
        if (path.startsWith("."))
            path = "internal" + path;

        this.path = path;
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
        return translation(serverLocale);
    }

    public String translation(CommandSender sender) {
        if ((sender instanceof ConsoleCommandSender && conf().shouldTranslateConsoleMessages())
                || sender instanceof Player)
            return translation();

        return defaultTranslation();
    }

    public String translation(SenderType type) {
        if ((type == SenderType.CONSOLE && conf().shouldTranslateConsoleMessages()) || type == SenderType.PLAYER)
            return translation();

        return defaultTranslation();
    }

    public String translation(Locale locale) {
        YamlConfiguration lang = LOCALE_CONFIGS.getOrDefault(locale, LOCALE_CONFIGS.get(DEFAULT_LOCALE));

        String out = lang.getString(path(), defaultTranslation());
        if (out == null || (this != Translation.MISC_NEWLINE && out.isEmpty()))
            out = defaultTranslation();

        return out;
    }

    public int version() {
        return Integer.parseInt(_version.translation());
    }

    public String locale() {
        return _locale.translation();
    }

    public void send(CommandSender sender, Object... args) {
        String formatted = String.format(translation(sender instanceof Player
                ? serverLocale
                : DEFAULT_LOCALE), args);

        String[] lines = formatted.split("<br>");
        for (String line : lines)
            MessageUtils.sendString(sender, line);
    }

    private Conf conf() {
        Bootstrap bootstrap = EnjinCraft.bootstrap();
        if (bootstrap == null)
            throw new IllegalStateException("Bootstrap not available");

        return bootstrap.getConfig();
    }

    public static void setServerLocale(Locale locale) {
        serverLocale = locale;
    }

    public static Map<Locale, String> localeNames() {
        return LOCALE_NAMES;
    }

    public static void loadLocales(Plugin plugin) {
        for (Locale locale : Locale.values()) {
            YamlConfiguration lang = locale.loadLocaleResource(plugin);
            if (lang == null)
                continue;

            setDefaults(lang);
            LOCALE_CONFIGS.put(locale, lang);
            LOCALE_NAMES.put(locale, lang.getString(Translation._language.path()));
        }
    }

    protected static void setDefaults(YamlConfiguration lang) {
        if (lang == null)
            return;

        for (Translation translation : values()) {
            if (!lang.isSet(translation.path)) {
                lang.set(translation.path, translation.def);
            } else {
                int argCount = getArgCount(lang.getString(translation.path));
                if (argCount != translation.argCount)
                    lang.set(translation.path, translation.def);
            }
        }
    }

    protected static int getArgCount(String text) {
        int argCount = 0;
        Matcher matcher = Pattern.compile("%s").matcher(text);
        while (matcher.find())
            argCount++;

        return argCount;
    }

}
