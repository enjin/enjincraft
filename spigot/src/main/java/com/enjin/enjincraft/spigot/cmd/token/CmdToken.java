package com.enjin.enjincraft.spigot.cmd.token;

import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.cmd.perm.CmdAddPerm;
import com.enjin.enjincraft.spigot.cmd.perm.CmdAddPermNFT;
import com.enjin.enjincraft.spigot.cmd.perm.CmdRevokePerm;
import com.enjin.enjincraft.spigot.cmd.perm.CmdRevokePermNFT;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;

public class CmdToken extends EnjCommand {

    public CmdToken(EnjCommand parent) {
        super(parent);
        this.aliases.add("token");
        this.requiredArgs.add("operation");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN)
                .withAllowedSenderTypes(SenderType.PLAYER, SenderType.CONSOLE)
                .build();
        this.subCommands.add(new CmdCreate(this));
        this.subCommands.add(new CmdUpdate(this));
        this.subCommands.add(new CmdDelete(this));
        this.subCommands.add(new CmdToInv(this));
        this.subCommands.add(new CmdNickname(this));
        this.subCommands.add(new CmdAddPerm(this));
        this.subCommands.add(new CmdAddPermNFT(this));
        this.subCommands.add(new CmdRevokePerm(this));
        this.subCommands.add(new CmdRevokePermNFT(this));
        this.subCommands.add(new CmdSetWalletView(this));
        this.subCommands.add(new CmdList(this));
        this.subCommands.add(new CmdExport(this));
        this.subCommands.add(new CmdImport(this));
    }

    @Override
    public void execute(CommandContext context) {
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_DESCRIPTION;
    }

}
