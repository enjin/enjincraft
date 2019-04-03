package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.service.identities.vo.Identity;
import com.enjin.enjincoin.sdk.service.tokens.vo.Token;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.inventory.WalletInventory;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.player.PlayerManager;
import com.enjin.enjincoin.spigot_framework.player.TokenData;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
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

            Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
                PlayerManager playerManager = this.main.getBootstrap().getPlayerManager();
                MinecraftPlayer minecraftPlayer = playerManager.getPlayer(player.getUniqueId());
                minecraftPlayer.reloadUser();

                Identity identity = minecraftPlayer.getIdentity();

//            System.out.println("WalletCommand.execute: player.getUniqueId() " + player.getUniqueId());
//            System.out.println("WalletCommand.execute: player Identity Id " + this.main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId()).getIdentityData().getId());
                List<TokenData> tokens = minecraftPlayer.getWallet().getTokens();
//            System.out.println("WalletCommand.execute: player # tokens found " + tokens.size());

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
