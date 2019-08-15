package com.enjin.ecmp.spigot;

import com.enjin.ecmp.spigot.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;

public class Messages {

    public static void identityNotLoaded(CommandSender target) {
        MessageUtils.sendString(target, "&cYour player data is loading. Try again momentarily.");
    }

    public static void identityNotLinked(CommandSender target) {
        MessageUtils.sendString(target, "&cYou have not linked an Enjin Wallet to your account.");
        linkInstructions(target);
    }

    public static void linkInstructions(CommandSender target) {
        MessageUtils.sendString(target, "&cType '/enj link' for instructions on how to link your Enjin Wallet.");
    }

    public static void allowanceNotSet(CommandSender target) {
        MessageUtils.sendString(target, "&cYou must approve the allowance request in your wallet before you can send or trade tokens.");
    }

    public static void error(CommandSender target, Exception ex) {
        MessageUtils.sendString(target, String.format("&cError: %s", ex.getMessage()));
    }

    public static void newLine(CommandSender target) {
        MessageUtils.sendString(target, "");
    }

}
