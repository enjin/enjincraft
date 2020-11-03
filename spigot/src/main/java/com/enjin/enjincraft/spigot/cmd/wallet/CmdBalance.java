package com.enjin.enjincraft.spigot.cmd.wallet;

import com.enjin.enjincraft.spigot.cmd.*;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.wallet.MutableBalance;
import org.bukkit.command.CommandSender;

import java.math.BigDecimal;

public class CmdBalance extends EnjCommand {

    public CmdBalance(CmdEnj parent) {
        super(parent);
        this.aliases.add("balance");
        this.aliases.add("bal");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_BALANCE)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();

        EnjPlayer enjPlayer = getValidSenderEnjPlayer(context);
        if (enjPlayer == null)
            return;

        BigDecimal ethBalance = enjPlayer.getEthBalance() == null
                ? BigDecimal.ZERO
                : enjPlayer.getEthBalance();
        BigDecimal enjBalance = enjPlayer.getEnjBalance() == null
                ? BigDecimal.ZERO
                : enjPlayer.getEnjBalance();

        Translation.COMMAND_BALANCE_WALLETADDRESS.send(sender, enjPlayer.getEthereumAddress());
        Translation.COMMAND_BALANCE_IDENTITYID.send(sender, enjPlayer.getIdentityId());

        if (enjBalance != null)
            Translation.COMMAND_BALANCE_ENJBALANCE.send(sender, enjBalance);
        if (enjBalance != null)
            Translation.COMMAND_BALANCE_ETHBALANCE.send(sender, ethBalance);

        TokenManager tokenManager = bootstrap.getTokenManager();

        int itemCount = 0;
        for (MutableBalance balance : enjPlayer.getTokenWallet().getBalances()) {
            if (balance.balance() == 0)
                continue;

            String fullId;
            try {
                fullId = TokenUtils.createFullId(balance.id(), balance.index());
            } catch (Exception e) {
                bootstrap.log(e);
                continue;
            }

            TokenModel tokenModel = tokenManager.getToken(fullId);
            if (tokenModel == null)
                continue;

            Translation.COMMAND_BALANCE_TOKENDISPLAY.send(sender, ++itemCount, tokenModel.getDisplayName(), balance.balance());
        }

        Translation.MISC_NEWLINE.send(sender);

        if (itemCount == 0)
            Translation.COMMAND_BALANCE_NOTOKENS.send(sender);
        else
            Translation.COMMAND_BALANCE_TOKENCOUNT.send(sender, itemCount);
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_BALANCE_DESCRIPTION;
    }

}
