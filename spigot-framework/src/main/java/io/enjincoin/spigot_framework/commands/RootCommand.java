package io.enjincoin.spigot_framework.commands;

import io.enjincoin.spigot_framework.BasePlugin;
import io.enjincoin.spigot_framework.commands.subcommands.LinkCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class RootCommand implements CommandExecutor {

    private final BasePlugin main;

    public RootCommand(BasePlugin main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            String sub = args[0];
            String[] subArgs = args.length == 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
            switch (sub.toLowerCase()) {
                case "link":
                    LinkCommand.execute(this.main, sender, subArgs);
                    break;
                default:
                    sender.sendMessage(String.format("No sub-command with alias %s exists.", sub));
                    break;
            }
        }
        return true;
    }
}
