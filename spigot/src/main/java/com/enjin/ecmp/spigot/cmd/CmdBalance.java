package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.configuration.TokenDefinition;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.wallet.MutableBalance;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CmdBalance extends EnjCommand {

    private SpigotBootstrap bootstrap;

    public CmdBalance(SpigotBootstrap bootstrap) {
        super();
        this.bootstrap = bootstrap;
        this.aliases.add("balance");
        this.aliases.add("bal");
    }

    @Override
    public void execute(CommandContext context) {
        EnjPlayer enjPlayer = context.enjPlayer;

        if (enjPlayer == null) return;

        CommandSender sender = context.sender;

        if (enjPlayer.isLinked()) {
            if (enjPlayer.isIdentityLoaded()) {
                BigDecimal ethBalance = (enjPlayer.getEthBalance() == null)
                        ? BigDecimal.ZERO
                        : enjPlayer.getEthBalance();
                BigDecimal enjBalance = (enjPlayer.getEnjBalance() == null)
                        ? BigDecimal.ZERO
                        : enjPlayer.getEnjBalance();

                sendMsg(sender, "EthAdr: " + ChatColor.LIGHT_PURPLE + enjPlayer.getEthereumAddress());
                sendMsg(sender, "ID: " + enjPlayer.getIdentityId() + "   ");

                if (enjBalance != null)
                    sendMsg(sender, ChatColor.GREEN + "[ " + enjBalance + " ENJ ] ");
                if (ethBalance != null)
                    sendMsg(sender, ChatColor.GREEN + "[ " + ethBalance + " ETH ]");

                int itemCount = 0;
                List<TextComponent> listing = new ArrayList<>();
                if (enjPlayer.isLinked()) {
                    List<MutableBalance> balances = enjPlayer.getTokenWallet().getBalances();
                    for (MutableBalance balance : balances) {
                        TokenDefinition def = bootstrap.getConfig().getTokens().get(balance.id());
                        if (def != null && balance != null && balance.balance() > 0) {
                            itemCount++;
                            listing.add(TextComponent.of(itemCount + ". ").color(TextColor.GOLD)
                                    .append(TextComponent.of(def.getDisplayName()).color(TextColor.DARK_PURPLE))
                                    .append(TextComponent.of(" (qty. " + balance.balance() + ")").color(TextColor.GREEN)));
                        }
                    }
                }

                sendMsg(sender, "");
                if (itemCount == 0)
                    sendMsg(sender, ChatColor.BOLD + "" + ChatColor.GOLD + "No CryptoItems found in your Enjin Wallet.");
                else
                    sendMsg(sender, ChatColor.BOLD + "" + ChatColor.GOLD + "Found " + itemCount + " CryptoItems in your Wallet: ");

                listing.forEach(l -> MessageUtils.sendComponent(sender, l));
            } else {
                TextComponent text = TextComponent.of("You have not linked a wallet to your account.")
                        .color(TextColor.RED);
                MessageUtils.sendComponent(sender, text);
            }
        } else {
            TextComponent text = TextComponent.of("You have not linked a wallet to your account.").color(TextColor.RED);
            MessageUtils.sendComponent(sender, text);
            text = TextComponent.of("Please type '/enj link' to link your account to your Enjin Wallet.").color(TextColor.RED);
            MessageUtils.sendComponent(sender, text);
        }
    }

    private void sendMsg(CommandSender sender, String msg) {
        TextComponent text = TextComponent.of(msg)
                .color(TextColor.GOLD);
        MessageUtils.sendComponent(sender, text);
    }

}
