package com.enjin.enjincraft.spigot.cmd.wallet.trade;

import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.cmd.arg.PlayerArgumentProcessor;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CmdAccept extends EnjCommand {

    public CmdAccept(CmdTrade cmdTrade) {
        super(cmdTrade.bootstrap(), cmdTrade);
        this.aliases.add("accept");
        this.requiredArgs.add(CmdTrade.PLAYER_ARG);
        this.requirements = new CommandRequirements.Builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_TRADE_ACCEPT)
                .build();
    }

    @Override
    public List<String> tab(CommandContext context) {
        if (context.args().size() == 1)
            return PlayerArgumentProcessor.INSTANCE.tab(context.sender(), context.args().get(0));

        return new ArrayList<>(0);
    }

    @Override
    public void execute(CommandContext context) {
        String target = context.args().get(0);

        EnjPlayer senderEnjPlayer = getValidSenderEnjPlayer(context);
        if (senderEnjPlayer == null)
            return;

        Player targetPlayer = getValidTargetPlayer(context, target);
        if (targetPlayer == null)
            return;

        EnjPlayer targetEnjPlayer = getValidTargetEnjPlayer(context, targetPlayer);
        if (targetEnjPlayer == null)
            return;

        try {
            boolean result = bootstrap.getTradeManager().acceptInvite(targetEnjPlayer, senderEnjPlayer);
            if (!result)
                Translation.COMMAND_TRADE_NOOPENINVITE.send(context.sender(), targetPlayer.getName());
        } catch (Exception e) {
            bootstrap.log(e);
        }
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TRADE_ACCEPT_DESCRIPTION;
    }

}
