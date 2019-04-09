package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * <p>Trade command handler.</p>
 */
public class MenuCommand {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin plugin;

    /**
     * <p>Empty line helper for MessageUtils.sendMessage</p>
     */
    private final TextComponent newline = TextComponent.of("");

    /**
     * <p>Link command handler constructor.</p>
     *
     * @param plugin the Spigot plugin
     */
    public MenuCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * <p>Executes and performs operations defined for the command.</p>
     *
     * @param sender the command sender
     * @param args   the command arguments
     *
     * @since 1.0
     */
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        final TextComponent.Builder component = TextComponent.builder().content("").color(TextColor.GRAY);
        component.append(TextComponent.builder().content(" ").build());
        component.append(TextComponent.builder().content("ENJ MENU").color(TextColor.DARK_PURPLE).build());
        component.append(TextComponent.builder().content(" ").build());
        component.append(TextComponent.builder().content(" Click to: ").build());
        component.append(TextComponent.builder().content("Link Status").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/enj link")).build());
        component.append(TextComponent.builder().content(" | ").build());
        component.append(TextComponent.builder().content("Open Wallet").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enj wallet")).build());
        component.append(TextComponent.builder().content(" | ").build());
        component.append(TextComponent.builder().content("Show Balance").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enj balance")).build());
        component.append(TextComponent.builder().content(" | ").build());
        component.append(TextComponent.builder().content("Trade CryptoItem").color(TextColor.DARK_AQUA).clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enj trade")).build());

        MessageUtils.sendMessage(sender, component.build());

        return;
    }
}
