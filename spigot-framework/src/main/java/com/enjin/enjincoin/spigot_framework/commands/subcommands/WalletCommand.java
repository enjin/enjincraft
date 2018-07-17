package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.sdk.client.service.tokens.vo.Token;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.inventory.WalletInventory;
import com.enjin.enjincoin.spigot_framework.player.TokenData;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

/**
 * <p>Wallet command handler.</p>
 */
public class WalletCommand {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    /**
     * <p>Wallet command handler constructor.</p>
     *
     * @param main the Spigot plugin
     */
    public WalletCommand(BasePlugin main) {
        this.main = main;
    }

    /**
     * <p>Executes and performs operations defined for the command.</p>
     *
     * @param sender the command sender
     * @param args the command arguments
     *
     * @since 1.0
     */
    public void execute(CommandSender sender, String[] args) {
        // TODO: Redesign around MinecraftPlayer
        if (sender instanceof Player) {
            Player player = (Player) sender;
            this.main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId()).reloadUser();

            Identity identity = this.main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId()).getIdentity();

            List<TokenData> tokens = this.main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId()).getWallet().getTokens();
            if (identity != null) {
                // we have an identity, but the wallet has not been linked yet.
                if (identity.getLinkingCode() != null) {
                    TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
                    MessageUtils.sendMessage(sender, text);
                    text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
                    MessageUtils.sendMessage(sender, text);
                    return;
                }

                Inventory inventory = WalletInventory.create(main, player, tokens);
                player.openInventory(inventory);
            } else {
                TextComponent text = TextComponent.of("You have not linked a wallet to your account.")
                        .color(TextColor.RED);
                MessageUtils.sendMessage(sender, text);
            }
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
