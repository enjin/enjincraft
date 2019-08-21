package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.Messages;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.configuration.TokenDefinition;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.i18n.Translation;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.wallet.MutableBalance;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CmdBalance extends EnjCommand {

    public CmdBalance(SpigotBootstrap bootstrap, CmdEnj parent) {
        super(bootstrap, parent);
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

        Translation.COMMAND_BALANCE_WALLETADDRESS.sendMessage(sender, enjPlayer.getEthereumAddress());
        Translation.COMMAND_BALANCE_IDENTITYID.sendMessage(sender, enjPlayer.getIdentityId());

        if (enjBalance != null)
            Translation.COMMAND_BALANCE_ENJBALANCE.sendMessage(sender, enjBalance);
        if (enjBalance != null)
            Translation.COMMAND_BALANCE_ETHBALANCE.sendMessage(sender, ethBalance);

        int itemCount = 0;
        List<String> tokenDisplays = new ArrayList<>();
        for (MutableBalance balance : enjPlayer.getTokenWallet().getBalances()) {
            if (balance == null || balance.balance() == 0) continue;
            TokenDefinition def = bootstrap.getConfig().getTokens().get(balance.id());
            if (def == null) continue;
            itemCount++;
            Translation.COMMAND_BALANCE_TOKENDISPLAY.sendMessage(sender, itemCount, def.getDisplayName(), balance.balance());
        }

        Messages.newLine(sender);
        if (itemCount == 0)
            Translation.COMMAND_BALANCE_NOTOKENS.sendMessage(sender);
        else
            Translation.COMMAND_BALANCE_TOKENCOUNT.sendMessage(sender, itemCount);

        tokenDisplays.forEach(l -> MessageUtils.sendString(sender, l));
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_BALANCE_DESCRIPTION;
    }

}
