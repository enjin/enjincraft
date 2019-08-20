package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.i18n.Translation;
import org.bukkit.command.CommandSender;

public class CmdHelp extends EnjCommand {

    public CmdHelp(SpigotBootstrap bootstrap, CmdEnj parent) {
        super(bootstrap, parent);
        this.aliases.add("help");
        this.aliases.add("h");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.ANY)
                .withPermission(Permission.CMD_HELP)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender;
        CmdEnj root = (CmdEnj) this.parent.get();

        root.showUsage(sender);
        root.cmdBalance.showUsage(sender);
        root.cmdHelp.showUsage(sender);
        root.cmdLink.showUsage(sender);
        root.cmdSend.showUsage(sender);
        root.cmdTrade.showUsage(sender);
        root.cmdTrade.cmdInvite.showUsage(sender);
        root.cmdTrade.cmdAccept.showUsage(sender);
        root.cmdTrade.cmdDecline.showUsage(sender);
        root.cmdUnlink.showUsage(sender);
        root.cmdWallet.showUsage(sender);
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_HELP_DESCRIPTION;
    }

}
