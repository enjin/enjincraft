package com.enjin.enjincoin.spigot_framework.commands;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.commands.subcommands.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RootCommand implements CommandExecutor {

    private final BasePlugin plugin;

    private final LinkCommand link;

    private final UnlinkCommand unlink;

    private final WalletCommand wallet;

    private final BalanceCommand balance;

    private final HelpCommand help;

    private final TradeCommand trade;

    private final MenuCommand menu;

    private final SidebarCommand sidebar;

    private final SendCommand send;

    private static Map<String, String> commands;

    public RootCommand(BasePlugin plugin) {
        this.commands = new HashMap<>();
        this.plugin = plugin;

        this.link = new LinkCommand(plugin);
        this.unlink = new UnlinkCommand(plugin);
        this.wallet = new WalletCommand(plugin);
        this.balance = new BalanceCommand(plugin);
        this.help = new HelpCommand(plugin);
        this.trade = new TradeCommand(plugin);
        this.menu = new MenuCommand(plugin);
        this.sidebar = new SidebarCommand(plugin);
        this.send = new SendCommand(plugin);
    }

    public Map<String, String> getCommandsMap() {
        return commands;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command.");
            return false;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            String sub = args[0];
            String[] subArgs = args.length == 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
            switch (sub.toLowerCase()) {
                case "link":
                    this.link.execute(sender, subArgs);
                    break;
                case "wallet":
                    this.wallet.execute(player, subArgs);
                    break;
                case "balance":
                    this.balance.execute(player, subArgs);
                    break;
                case "help":
                    this.help.execute(player);
                    break;
                case "unlink":
                    this.unlink.execute(player, subArgs);
                    break;
                case "trade":
                    this.trade.execute(player, subArgs);
                    break;
                case "menu":
                    this.menu.execute(player, subArgs);
                    break;
                case "scoreboard":
                    this.sidebar.execute(player, subArgs);
                    break;
                case "send":
                    this.send.execute(player, subArgs);
                    break;
                default:
                    player.sendMessage(String.format("No sub-command with alias %s exists.", sub));
                    this.help.execute(player);
                    this.menu.execute(player, subArgs);
                    break;
            }
        } else {
            this.help.execute(sender);
        }
        return true;
    }
}
