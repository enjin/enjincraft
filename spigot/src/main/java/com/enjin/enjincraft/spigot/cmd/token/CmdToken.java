package com.enjin.enjincraft.spigot.cmd.token;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;

public class CmdToken extends EnjCommand {

    public CmdToken(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("token");
        this.requiredArgs.add("operation");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN)
                .withAllowedSenderTypes(SenderType.PLAYER, SenderType.CONSOLE)
                .build();
        this.subCommands.add(new CmdCreate(bootstrap, this));
        this.subCommands.add(new CmdUpdate(bootstrap, this));
        this.subCommands.add(new CmdDelete(bootstrap, this));
        this.subCommands.add(new CmdToInv(bootstrap, this));
        this.subCommands.add(new CmdNickname(bootstrap, this));
        this.subCommands.add(new CmdAddPerm(bootstrap, this));
        this.subCommands.add(new CmdAddPermNFT(bootstrap, this));
        this.subCommands.add(new CmdRevokePerm(bootstrap, this));
        this.subCommands.add(new CmdRevokePermNFT(bootstrap, this));
        this.subCommands.add(new CmdSetWalletView(bootstrap, this));
        this.subCommands.add(new CmdList(bootstrap, this));
        this.subCommands.add(new CmdExport(bootstrap, this));
        this.subCommands.add(new CmdImport(bootstrap, this));
    }

    @Override
    public void execute(CommandContext context) {
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_DESCRIPTION;
    }

}
