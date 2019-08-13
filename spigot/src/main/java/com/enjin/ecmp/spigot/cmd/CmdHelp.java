package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import org.bukkit.ChatColor;

public class CmdHelp extends EnjCommand {

    private SpigotBootstrap bootstrap;

    public CmdHelp(SpigotBootstrap bootstrap) {
        super();
        this.bootstrap = bootstrap;
        this.aliases.add("help");
        this.aliases.add("h");
    }

    @Override
    public void execute(CommandContext context) {
        context.sender.sendMessage("Usage:");
        context.sender.sendMessage("/enj [command]");
        context.sender.sendMessage("");
        context.sender.sendMessage(ChatColor.GOLD + "/enj link: " + ChatColor.WHITE + "Display linking code or linked address if available.");
        context.sender.sendMessage(ChatColor.GOLD + "/enj unlink: " + ChatColor.WHITE + "Removes the link to an Ethereum Wallet.");
        context.sender.sendMessage(ChatColor.GOLD + "/enj balance: " + ChatColor.WHITE + "Display wallet Address, Ethereum and Enjin Coin balances, as well as a list of balance CryptoItems.");
        context.sender.sendMessage(ChatColor.GOLD + "/enj wallet: " + ChatColor.WHITE + "Opens a wallet inventory panel which allows for checkout of balance CryptoItems.");
        context.sender.sendMessage(ChatColor.GOLD + "/enj send <player>: " + ChatColor.WHITE + "Sends the held token to another player.");
        context.sender.sendMessage(ChatColor.GOLD + "/enj trade invite <player>: " + ChatColor.WHITE + "Send a trade invite to another player.");
        context.sender.sendMessage(ChatColor.GOLD + "/enj scoreboard: " + ChatColor.WHITE + "Displays/hides the ENJ Scoreboard.");
    }

}
