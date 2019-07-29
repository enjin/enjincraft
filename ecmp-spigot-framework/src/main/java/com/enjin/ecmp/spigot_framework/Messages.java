package com.enjin.ecmp.spigot_framework;

import com.enjin.ecmp.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;

public class Messages {

    public static void tokenSent(CommandSender target, String amount, String name) {
        TextComponent text = TextComponent.of("You have sent ").color(TextColor.GOLD)
                .append(TextComponent.of(amount + "x ").color(TextColor.GREEN))
                .append(TextComponent.of(name).color(TextColor.DARK_PURPLE));
        MessageUtils.sendMessage(target, text);
    }

    public static void tokenReceived(CommandSender target, String amount, String name) {
        TextComponent text = TextComponent.of("You have received ").color(TextColor.GOLD)
                .append(TextComponent.of(amount + "x ").color(TextColor.GREEN))
                .append(TextComponent.of(name).color(TextColor.DARK_PURPLE));
        MessageUtils.sendMessage(target, text);
    }

}
