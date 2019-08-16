package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.Messages;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.configuration.TokenDefinition;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.wallet.MutableBalance;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CmdBalance extends EnjCommand {

    public CmdBalance(SpigotBootstrap bootstrap) {
        super(bootstrap);
        this.aliases.add("balance");
        this.aliases.add("bal");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_BALANCE)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        Player sender = context.player;
        EnjPlayer enjPlayer = context.enjPlayer;

        if (!enjPlayer.isLinked()) {
            Messages.identityNotLinked(sender);
            return;
        }

        BigDecimal ethBalance = (enjPlayer.getEthBalance() == null)
                ? BigDecimal.ZERO
                : enjPlayer.getEthBalance();
        BigDecimal enjBalance = (enjPlayer.getEnjBalance() == null)
                ? BigDecimal.ZERO
                : enjPlayer.getEnjBalance();

        MessageUtils.sendString(sender, String.format("&6Wallet Address: &d%s", enjPlayer.getEthereumAddress()));
        MessageUtils.sendString(sender, String.format("&6Identity ID: &d%s", enjPlayer.getIdentityId()));

        if (enjBalance != null)
            MessageUtils.sendString(sender, String.format("&a[ %s ENJ ]", enjBalance));
        if (enjBalance != null)
            MessageUtils.sendString(sender, String.format("&a[ %s ETH ]", ethBalance));

        int itemCount = 0;
        List<String> tokenDisplays = new ArrayList<>();
        for (MutableBalance balance : enjPlayer.getTokenWallet().getBalances()) {
            if (balance == null || balance.balance() == 0) continue;
            TokenDefinition def = bootstrap.getConfig().getTokens().get(balance.id());
            if (def == null) continue;
            itemCount++;
            tokenDisplays.add(String.format("&6%s. &5%s &a(qty. %s)", itemCount, def.getDisplayName(), balance.balance()));
        }

        Messages.newLine(sender);
        if (itemCount == 0)
            MessageUtils.sendString(sender, "&l&6No tokens found in your Enjin Wallet.");
        else
            MessageUtils.sendString(sender, String.format("&l&6Found %s tokens in your Wallet:", itemCount));

        tokenDisplays.forEach(l -> MessageUtils.sendString(sender, l));
    }

}
