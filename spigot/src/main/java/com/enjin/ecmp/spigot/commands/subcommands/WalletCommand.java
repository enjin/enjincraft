package com.enjin.ecmp.spigot.commands.subcommands;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.ECPlayer;
import com.enjin.ecmp.spigot.player.PlayerManager;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.wallet.TokenWalletView;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WalletCommand {

    private SpigotBootstrap bootstrap;

    public WalletCommand(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            Bukkit.getScheduler().runTaskAsynchronously(bootstrap.plugin(), () -> {
                PlayerManager playerManager = bootstrap.getPlayerManager();
                ECPlayer enjinCoinPlayer = playerManager.getPlayer(player);

                if (!enjinCoinPlayer.isLinked()) {
                    TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
                    MessageUtils.sendComponent(sender, text);
                    text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
                    MessageUtils.sendComponent(sender, text);
                    return;
                }

                if (enjinCoinPlayer.getTokenWallet() == null) {
                    TextComponent text = TextComponent.of("Your wallet balances are loading, try again in a few seconds.")
                            .color(TextColor.RED);
                    MessageUtils.sendComponent(sender, text);
                    return;
                }

                if (enjinCoinPlayer.isIdentityLoaded()) {
                    // we have an identity, but the wallet has not been linked yet.
                    if (!enjinCoinPlayer.isLinked()) {
                        TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
                        MessageUtils.sendComponent(sender, text);
                        text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
                        MessageUtils.sendComponent(sender, text);
                        return;
                    }

//                    Inventory inventory = LegacyWalletInventory.create(plugin, player, tokens);
//                    player.openInventory(inventory);

                    TokenWalletView view = new TokenWalletView(bootstrap, enjinCoinPlayer);
                    view.open(player);
                } else {
                    TextComponent text = TextComponent.of("You have not linked a wallet to your account.")
                            .color(TextColor.RED);
                    MessageUtils.sendComponent(sender, text);
                }
            });
        } else {
            TextComponent text = TextComponent.of("Only players can use this command.")
                    .color(TextColor.RED);
            MessageUtils.sendComponent(sender, text);
        }
    }

    private void sendMsg(CommandSender sender, String msg) {
        TextComponent text = TextComponent.of(msg)
                .color(TextColor.RED);
        MessageUtils.sendComponent(sender, text);
    }

}
