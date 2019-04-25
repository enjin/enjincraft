package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.wallet.WalletInventory;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.player.PlayerManager;
import com.enjin.enjincoin.spigot_framework.wallet.Balance;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class WalletCommand {

    private BasePlugin plugin;

    public WalletCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
                MinecraftPlayer minecraftPlayer = playerManager.getPlayer(player.getUniqueId());
                minecraftPlayer.reloadUser();

                Identity identity = minecraftPlayer.getIdentity();

                List<Balance> tokens = minecraftPlayer.getWallet().getTokens();

                if (identity != null) {
                    // we have an identity, but the wallet has not been linked yet.
                    if (identity.getLinkingCode() != null) {
                        TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
                        MessageUtils.sendMessage(sender, text);
                        text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
                        MessageUtils.sendMessage(sender, text);
                        return;
                    }

                    Inventory inventory = WalletInventory.create(plugin, player, tokens);
                    player.openInventory(inventory);
                } else {
                    TextComponent text = TextComponent.of("You have not linked a wallet to your account.")
                            .color(TextColor.RED);
                    MessageUtils.sendMessage(sender, text);
                }
            });
        } else {
            TextComponent text = TextComponent.of("Only players can use this command.")
                    .color(TextColor.RED);
            MessageUtils.sendMessage(sender, text);
        }
    }

    private void sendMsg(CommandSender sender, String msg) {
        TextComponent text = TextComponent.of(msg)
                .color(TextColor.RED);
        MessageUtils.sendMessage(sender, text);
    }

}
