package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.util.MessageUtils;
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

        MessageUtils.sendString(sender, root.getUsage(context));
        MessageUtils.sendString(sender, root.cmdBalance.getUsage(context));
        MessageUtils.sendString(sender, root.cmdHelp.getUsage(context));
        MessageUtils.sendString(sender, root.cmdLink.getUsage(context));
        MessageUtils.sendString(sender, root.cmdSend.getUsage(context));
        MessageUtils.sendString(sender, root.cmdTrade.cmdInvite.getUsage(context));
        MessageUtils.sendString(sender, root.cmdTrade.cmdAccept.getUsage(context));
        MessageUtils.sendString(sender, root.cmdTrade.cmdDecline.getUsage(context));
        MessageUtils.sendString(sender, root.cmdUnlink.getUsage(context));
        MessageUtils.sendString(sender, root.cmdWallet.getUsage(context));

//        MessageUtils.sendString(sender, "Usage:");
//        MessageUtils.sendString(sender, "/enj [command]");
//        Messages.newLine(sender);
//        MessageUtils.sendString(sender, "&6/enj link: &fDisplay linking code or linked address if available.");
//        MessageUtils.sendString(sender, "&6/enj unlink: &fRemoves the link to an Ethereum Wallet.");
//        MessageUtils.sendString(sender, "&6/enj balance: &fDisplay wallet address, eth, enj, and token balances.");
//        MessageUtils.sendString(sender, "&6/enj wallet: &fOpens a wallet menu where tokens can be checked out.");
//        MessageUtils.sendString(sender, "&6/enj send <player>: &fSends the held token to another player.");
//        MessageUtils.sendString(sender, "&6/enj trade invite <player>: &fSend a trade invite to another player.");
    }

}
