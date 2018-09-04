package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.inventory.WalletInventory;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.player.TokenData;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import com.google.gson.JsonObject;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>Balance command handler.</p>
 */
public class BalanceCommand {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    /**
     * <p>Balance command handler constructor.</p>
     *
     * @param main the Spigot plugin
     */
    public BalanceCommand(BasePlugin main) {
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
        if (sender instanceof Player) {
            Player player = (Player) sender;

            MinecraftPlayer mcPlayer = this.main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId());
            // reload/refresh user info

            Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
                mcPlayer.reloadUser();

                boolean showAll = false;
                Identity identity = mcPlayer.getIdentity();

                if (identity.getLinkingCode() != null) {
                    TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
                    MessageUtils.sendMessage(sender, text);
                    text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
                    MessageUtils.sendMessage(sender, text);
                    return;
                }

                if (identity != null) {
                    Double ethBalance = (mcPlayer.getIdentityData().getEthBalance() == null) ? 0 : mcPlayer.getIdentityData().getEthBalance();
                    Double enjBalance = (mcPlayer.getIdentityData().getEnjBalance() == null) ? 0 : mcPlayer.getIdentityData().getEnjBalance();

                    sendMsg(sender, "EthAdr: " + ChatColor.LIGHT_PURPLE + identity.getEthereumAddress());
                    sendMsg(sender, "ID: " + identity.getId() + "   ");

                    if (enjBalance > 0)
                        sendMsg(sender, ChatColor.GREEN + "[ " + enjBalance  + " ENJ ] " );
                    if (ethBalance > 0)
                        sendMsg(sender, ChatColor.GREEN + "[ " + ethBalance + " ETH ]");

                    JsonObject tokensDisplayConfig = main.getBootstrap().getConfig().get("tokens").getAsJsonObject();
                    int itemCount = 0;
                    List<TextComponent> listing = new ArrayList<>();
                    for(int i = 0; i < identity.getTokens().size(); i++) {
                        JsonObject tokenDisplay = tokensDisplayConfig.has(String.valueOf(identity.getTokens().get(i).getTokenId()))
                                ? tokensDisplayConfig.get(String.valueOf(identity.getTokens().get(i).getTokenId())).getAsJsonObject()
                                : null;
                        Double balance = identity.getTokens().get(i).getBalance();
                        if (tokenDisplay != null) {
                            if (balance > 0)
                            {
                                itemCount++;
                                if (tokenDisplay != null && tokenDisplay.has("displayName")) {
                                    listing.add(TextComponent.of(String.valueOf(itemCount) + ". ").color(TextColor.GOLD)
                                            .append(TextComponent.of(tokenDisplay.get("displayName").getAsString()).color(TextColor.DARK_PURPLE))
                                            .append(TextComponent.of(" (qty. " + balance + ")").color(TextColor.GREEN)));
                                }
                            }
                        } else if (showAll) {
                            if (balance > 0) {
                                itemCount++;

                                listing.add(TextComponent.of(String.valueOf(itemCount) + ". ").color(TextColor.GOLD)
                                        .append(TextComponent.of(identity.getTokens().get(i).getName()).color(TextColor.DARK_PURPLE))
                                        .append(TextComponent.of(" (qty. " + balance + ")").color(TextColor.GREEN)));
                            }
                        }
                    }

                    sendMsg(sender, "");
                    if (itemCount == 0)
                        sendMsg(sender, ChatColor.BOLD + "" + ChatColor.GOLD + "No CryptoItems found in your Enjin Wallet.");
                    else
                        sendMsg(sender,  ChatColor.BOLD + "" + ChatColor.GOLD + "Found " + itemCount + " CryptoItems in Wallet: ");

                    listing.forEach( l -> MessageUtils.sendMessage(sender, l) );


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
