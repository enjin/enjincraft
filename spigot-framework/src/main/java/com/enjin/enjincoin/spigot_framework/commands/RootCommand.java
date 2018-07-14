package com.enjin.enjincoin.spigot_framework.commands;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.commands.subcommands.BalanceCommand;
import com.enjin.enjincoin.spigot_framework.commands.subcommands.HelpCommand;
import com.enjin.enjincoin.spigot_framework.commands.subcommands.LinkCommand;
import com.enjin.enjincoin.spigot_framework.commands.subcommands.WalletCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
        this.wallet = new WalletCommand(main);
        this.balance = new BalanceCommand(main);
        this.help = new HelpCommand(main);
    }

    public Map<String, String> getCommandsMap() { return commands; }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
                    this.help.execute(sender, subArgs);
                    break;
                default:
                    sender.sendMessage(String.format("No sub-command with alias %s exists.", sub));
                    sender.sendMessage("Usage:");
                    sender.sendMessage("/enj [command]");
                    sender.sendMessage("");
                    sender.sendMessage(ChatColor.GOLD + "/enj balance: " + ChatColor.WHITE + "Display wallet Address, Ethereum and Enjin Coin balances, as well as a list of owned CryptoItems.");
                    sender.sendMessage(ChatColor.GOLD + "/enj link: " + ChatColor.WHITE + "Display linking code or linked address if available.");
                    sender.sendMessage(ChatColor.GOLD + "/enj wallet: " + ChatColor.WHITE + "Opens a wallet inventory panel which allows for checkout of owned CryptoItems.");
                    break;
            }
        } else {
            sender.sendMessage("Usage:");
            sender.sendMessage("/enj [command]");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "/enj balance: " + ChatColor.WHITE + "Display wallet Address, Ethereum and Enjin Coin balances, as well as a list of owned CryptoItems.");
            sender.sendMessage(ChatColor.GOLD + "/enj link: " + ChatColor.WHITE + "Display linking code or linked address if available.");
            sender.sendMessage(ChatColor.GOLD + "/enj wallet: " + ChatColor.WHITE + "Opens a wallet inventory panel which allows for checkout of owned CryptoItems.");
        }
        return true;
    }
}
