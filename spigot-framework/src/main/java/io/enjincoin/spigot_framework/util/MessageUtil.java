package io.enjincoin.spigot_framework.util;

import net.kyori.text.TextComponent;
import net.kyori.text.serializer.ComponentSerializers;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.List;

public class MessageUtil {

    public static void sendMessage(CommandSender sender, TextComponent component) {
        String json = ComponentSerializers.JSON.serialize(component);
        BaseComponent[] components = ComponentSerializer.parse(json);
        sender.spigot().sendMessage(components);
    }

    public static void sendMessages(CommandSender sender, TextComponent... components) {
        if (components != null) {
            for (TextComponent component : components) {
                sendMessage(sender, component);
            }
        }
    }

    public static void sendMessages(CommandSender sender, List<TextComponent> components) {
        if (components != null) {
            sendMessages(sender, components.toArray(new TextComponent[]{}));
        }
    }

}
