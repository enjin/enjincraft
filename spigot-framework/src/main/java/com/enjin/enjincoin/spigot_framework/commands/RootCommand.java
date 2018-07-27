package com.enjin.enjincoin.spigot_framework.commands;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.commands.subcommands.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * <p>Root command handler.</p>
 *
 * @since 1.0
 */
public class RootCommand implements CommandExecutor {

    /**
     * <p>The spigot plugin.</p>
     */
    private final BasePlugin main;

    /**
     * <p>Link command handler instance.</p>
     */
    private final LinkCommand link;

    /**
     * <p>Unlink command handler instance.</p>
     */
    private final UnlinkCommand unlink;

    /**
     * <p>Wallet command handler instance.</p>
     */
    private final WalletCommand wallet;

    /**
     * <p>Balance command handler instance.</p>
     */
    private final BalanceCommand balance;

    /**
     * <p>Help command handler instance.</p>
     */
    private final HelpCommand help;

    /**
     * <p>Trade command handler instance.</p>
     */
    private final TradeCommand trade;

    /**
     * <p>Provides a click-text menu for accessing commands</p>
     */
    private final MenuCommand menu;

    /**
     * <p>commands list and details</p>
     * key is command name
     * value is command help body
     */
    private static Map<String, String> commands;

    /**
     * <p>Root command handler constructor.</p>
     *
     * @param main the Spigot plugin
     */
    public RootCommand(BasePlugin main) {
        this.commands = new HashMap<>();
        this.main = main;

        this.link = new LinkCommand(main);
        this.unlink = new UnlinkCommand(main);
        this.wallet = new WalletCommand(main);
        this.balance = new BalanceCommand(main);
        this.help = new HelpCommand(main);
        this.trade = new TradeCommand(main);
        this.menu = new MenuCommand(main);
    }

    public Map<String, String> getCommandsMap() { return commands; }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command.");
            return false;
        }

        if (args.length > 0) {
            String sub = args[0];
            String[] subArgs = args.length == 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
            switch (sub.toLowerCase()) {
                case "link":
                    this.link.execute(sender, subArgs);
                    break;
                case "wallet": // TODO: Refactor wallet command around MinecraftPlayer
                    this.wallet.execute(sender, subArgs);
                    break;
                case "balance":
                    this.balance.execute(sender, subArgs);
                    break;
                case "help":
                    this.help.execute(sender);
                    break;
                case "unlink":
                    this.unlink.execute(sender, subArgs);
                    break;
                case "trade":
                    this.trade.execute(sender, subArgs);
                    break;
                case "menu":
                    this.menu.execute(sender, subArgs);
                default:
                    sender.sendMessage(String.format("No sub-command with alias %s exists.", sub));
                    this.help.execute(sender);
                    this.menu.execute(sender, subArgs);
                    break;
            }
        } else {
            this.help.execute(sender);
        }
        return true;
    }
}
