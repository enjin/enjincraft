package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class HelpCommand {

    private BasePlugin plugin;

    public HelpCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender) {
        sender.sendMessage("Usage:");
        sender.sendMessage("/enj [command]");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "/enj balance: " + ChatColor.WHITE + "Display wallet Address, Ethereum and Enjin Coin balances, as well as a list of owned CryptoItems.");
        sender.sendMessage(ChatColor.GOLD + "/enj link: " + ChatColor.WHITE + "Display linking code or linked address if available.");
        sender.sendMessage(ChatColor.GOLD + "/enj scoreboard: " + ChatColor.WHITE + "Displays/hides the ENJ Scoreboard.");
        sender.sendMessage(ChatColor.GOLD + "/enj trade invite <player>: " + ChatColor.WHITE + "Send a trade invite to another player.");
        sender.sendMessage(ChatColor.GOLD + "/enj unlink: " + ChatColor.WHITE + "Removes the link to an Ethereum Wallet.");
        sender.sendMessage(ChatColor.GOLD + "/enj wallet: " + ChatColor.WHITE + "Opens a wallet inventory panel which allows for checkout of owned CryptoItems.");
    }

    private void sendMsg(CommandSender sender, String msg) {
        TextComponent text = TextComponent.of(msg)
                .color(TextColor.GOLD);
        MessageUtils.sendMessage(sender, text);
    }

}
