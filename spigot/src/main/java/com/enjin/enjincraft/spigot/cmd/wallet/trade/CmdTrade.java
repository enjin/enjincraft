package com.enjin.enjincraft.spigot.cmd.wallet.trade;

import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;

public class CmdTrade extends EnjCommand {

    public static final String PLAYER_ARG = "player";

    public CmdTrade(EnjCommand parent) {
        super(parent);
        this.aliases.add("trade");
        this.requiredArgs.add("action");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_TRADE)
                .build();
        this.addSubCommand(new CmdInvite(this));
        this.addSubCommand(new CmdAccept(this));
        this.addSubCommand(new CmdDecline(this));
    }

    @Override
    public void execute(CommandContext context) {
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TRADE_DESCRIPTION;
    }

}
