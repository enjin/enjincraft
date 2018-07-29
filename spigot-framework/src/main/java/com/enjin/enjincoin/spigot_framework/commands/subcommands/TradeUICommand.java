package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.inventory.PlayerSelection;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.player.PlayerManager;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import com.enjin.minecraft_commons.spigot.ui.MenuAPI;
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
public class TradeUICommand {

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
    public TradeUICommand(BasePlugin plugin) {
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
    public void execute(Player sender, String[] args) {
        PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
        MinecraftPlayer minecraftPlayer = playerManager.getPlayer(sender.getUniqueId());

        if (minecraftPlayer == null) {
            return;
        }

        PlayerSelection playerSelectionMenu = new PlayerSelection(sender.getUniqueId());
        playerSelectionMenu.openMenu(sender);
    }

    private void errorInvalidUuid(CommandSender sender) {
        final TextComponent text = TextComponent.of("The UUID provided is invalid.")
                .color(TextColor.RED);

        MessageUtils.sendMessage(sender, newline);
        MessageUtils.sendMessage(sender, text);
    }

    /**
     * method should just provide user feedback that the player's identity is already unlinked then provide
     * the linking code for them to use to link a wallet.
     *
     * @param sender
     * @param address
     */
    private void handleError(CommandSender sender, String address) {
        TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
        MessageUtils.sendMessage(sender, text);
        text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
        MessageUtils.sendMessage(sender, text);
    }
}
