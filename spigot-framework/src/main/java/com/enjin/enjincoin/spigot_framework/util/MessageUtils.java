package com.enjin.enjincoin.spigot_framework.util;

import net.kyori.text.TextComponent;
import net.kyori.text.serializer.ComponentSerializers;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * <p>Message related operations.</p>
 *
 * <p>This class tries to handle {@code null} input gracefully.
 * An exception will generally not be thrown for a {@code null} input.
 * Each method documents its behaviour in detail.</p>
 *
 * <p>#ThreadSafe#</p>
 *
 * @since 1.0
 */
public class MessageUtils {

    /**
     * <p>{@code MessageUtils} instances should NOT be constructed in standard programming.</p>
     *
     * <p>This constructor is public to permit tools that require a JavaBean instance
     * to operate.</p>
     */
    public MessageUtils() {
        super();
    }

    /**
     * <p>Sends a {@code TextComponent} message to a {@code CommandSender}.</p>
     *
     * @param sender the {@code CommandSender} to send the message to
     * @param component the {@code TextComponent} message to send to the sender
     *
     * @since 1.0
     */
    public static void sendMessage(CommandSender sender, TextComponent component) {
        String json = ComponentSerializers.JSON.serialize(component);
        BaseComponent[] components = ComponentSerializer.parse(json);
        sender.spigot().sendMessage(components);
    }

    /**
     * <p>Sends an array of {@code TextComponent} messages to a {@code CommandSender}.</p>
     *
     * @param sender the {@code CommandSender} to send the message to
     * @param components the array of {@code TextComponent} messages to send to the sender
     *
     * @since 1.0
     */
    public static void sendMessages(CommandSender sender, TextComponent... components) {
        if (components != null) {
            for (TextComponent component : components) {
                sendMessage(sender, component);
            }
        }
    }

    /**
     * <p>Sends a list of {@code TextComponent} messages to a {@code CommandSender}.</p>
     *
     * @param sender the {@code CommandSender} to send the message to
     * @param components the list of {@code TextComponent} messages to send to the sender
     *
     * @since 1.0
     */
    public static void sendMessages(CommandSender sender, List<TextComponent> components) {
        if (components != null) {
            sendMessages(sender, components.toArray(new TextComponent[]{}));
        }
    }

}
