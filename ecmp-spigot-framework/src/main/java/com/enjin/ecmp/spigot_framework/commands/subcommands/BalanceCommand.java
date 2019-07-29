package com.enjin.ecmp.spigot_framework.commands.subcommands;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.ecmp.spigot_framework.player.MinecraftPlayer;
import com.enjin.ecmp.spigot_framework.util.MessageUtils;
import com.google.gson.JsonObject;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BalanceCommand {

    private BasePlugin plugin;

    public BalanceCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            MinecraftPlayer mcPlayer = this.plugin.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId());
            // reload/refresh user info

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                mcPlayer.reloadUser();

                boolean showAll = false;
                if (!mcPlayer.isLinked()) {
                    TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
                    MessageUtils.sendMessage(sender, text);
                    text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
                    MessageUtils.sendMessage(sender, text);
                    return;
                }

                if (mcPlayer.isIdentityLoaded()) {
                    BigDecimal ethBalance = (mcPlayer.getEthBalance() == null)
                            ? BigDecimal.ZERO
                            : mcPlayer.getEthBalance();
                    BigDecimal enjBalance = (mcPlayer.getEnjBalance() == null)
                            ? BigDecimal.ZERO
                            : mcPlayer.getEnjBalance();

                    sendMsg(sender, "EthAdr: " + ChatColor.LIGHT_PURPLE + mcPlayer.getEthereumAddress());
                    sendMsg(sender, "ID: " + mcPlayer.getIdentityId() + "   ");

                    if (enjBalance != null)
                        sendMsg(sender, ChatColor.GREEN + "[ " + enjBalance + " ENJ ] ");
                    if (ethBalance != null)
                        sendMsg(sender, ChatColor.GREEN + "[ " + ethBalance + " ETH ]");

                    JsonObject tokensDisplayConfig = plugin.getBootstrap().getConfig().getRoot().get("tokens").getAsJsonObject();
                    int itemCount = 0;
                    List<TextComponent> listing = new ArrayList<>();
//                    if (identity.getTokens() != null) {
//                        for (int i = 0; i < identity.getTokens().size(); i++) {
//                            JsonObject tokenDisplay = tokensDisplayConfig.has(String.valueOf(identity.getTokens().get(i).getTokenId()))
//                                    ? tokensDisplayConfig.get(String.valueOf(identity.getTokens().get(i).getTokenId())).getAsJsonObject()
//                                    : null;
//                            Integer balance = identity.getTokens().get(i).getBalance();
//                            if (balance != null && balance > 0) {
//                                if (tokenDisplay != null) {
//                                    itemCount++;
//                                    if (tokenDisplay != null && tokenDisplay.has("displayName")) {
//                                        listing.add(TextComponent.of(itemCount + ". ").color(TextColor.GOLD)
//                                                .append(TextComponent.of(tokenDisplay.get("displayName").getAsString()).color(TextColor.DARK_PURPLE))
//                                                .append(TextComponent.of(" (qty. " + balance + ")").color(TextColor.GREEN)));
//                                    }
//                                } else if (showAll) {
//                                    itemCount++;
//
//                                    listing.add(TextComponent.of(itemCount + ". ").color(TextColor.GOLD)
//                                            .append(TextComponent.of(identity.getTokens().get(i).getName()).color(TextColor.DARK_PURPLE))
//                                            .append(TextComponent.of(" (qty. " + balance + ")").color(TextColor.GREEN)));
//                                }
//                            }
//                        }
//                    }

                    sendMsg(sender, "");
                    if (itemCount == 0)
                        sendMsg(sender, ChatColor.BOLD + "" + ChatColor.GOLD + "No CryptoItems found in your Enjin Wallet.");
                    else
                        sendMsg(sender, ChatColor.BOLD + "" + ChatColor.GOLD + "Found " + itemCount + " CryptoItems in your Wallet: ");

                    listing.forEach(l -> MessageUtils.sendMessage(sender, l));


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
                .color(TextColor.GOLD);
        MessageUtils.sendMessage(sender, text);
    }

}
