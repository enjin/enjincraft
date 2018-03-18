package io.enjincoin.spigot_framework.commands;

import io.enjincoin.spigot_framework.BasePlugin;
import io.enjincoin.spigot_framework.commands.subcommands.LinkCommand;
import io.enjincoin.spigot_framework.commands.subcommands.WalletCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class RootCommand implements CommandExecutor {

    private final BasePlugin main;
    private final LinkCommand link;
    private final WalletCommand wallet;

    public RootCommand(BasePlugin main) {
        this.main = main;
        this.link = new LinkCommand(main);
        this.wallet = new WalletCommand(main);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            String sub = args[0];
            String[] subArgs = args.length == 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
            switch (sub.toLowerCase()) {
                case "link":
                    this.link.execute(sender, subArgs);
                    break;
                case "wallet":
                    this.wallet.execute(sender, subArgs);
                    break;
                default:
                    sender.sendMessage(String.format("No sub-command with alias %s exists.", sub));
                    break;
            }
        }
        return true;
    }
}
