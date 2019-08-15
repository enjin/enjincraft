package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.Messages;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.util.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class EnjCommand {

    protected SpigotBootstrap bootstrap;
    protected List<String> aliases;
    protected List<EnjCommand> subCommands;
    protected List<SenderType> allowedSenderTypes;

    public EnjCommand(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        this.aliases = new ArrayList<>();
        this.subCommands = new ArrayList<>();
        this.allowedSenderTypes = new ArrayList<>();
        setAllowedSenderTypes(SenderType.ANY);
    }

    public abstract void execute(CommandContext context);

    public void process(CommandContext context) {
        try {
            if (!allowedSenderTypes.contains(SenderType.ANY) && !allowedSenderTypes.contains(context.senderType)) {
                sendInvalidSenderTypeMessage(context);
                return;
            }

            if (context.args.size() > 0) {
                for (EnjCommand subCommand : subCommands) {
                    if (subCommand.aliases.contains(context.args.get(0).toLowerCase())) {
                        context.args.remove(0);
                        context.commandStack.add(this);
                        subCommand.process(context);
                        return;
                    }
                }
            }

            execute(context);
        } catch (Exception ex) {
            ex.printStackTrace();
            Messages.error(context.sender, ex);
        }
    }

    protected void setAllowedSenderTypes(SenderType... types) {
        allowedSenderTypes.clear();
        for (SenderType type : types) allowedSenderTypes.add(type);
    }

    protected void addSubCommand(EnjCommand subCommand) {
        this.subCommands.add(subCommand);
    }

    private void sendInvalidSenderTypeMessage(CommandContext context) {
        if (context.senderType == SenderType.PLAYER)
            MessageUtils.sendString(context.sender, "&cThis command cannot be used by players.");
        else if (context.senderType == SenderType.CONSOLE)
            MessageUtils.sendString(context.sender, "&cThis command cannot be used by the console.");
        else if (context.senderType == SenderType.REMOTE_CONSOLE)
            MessageUtils.sendString(context.sender, "&cThis command cannot be used remotely.");
        else if (context.senderType == SenderType.BLOCK)
            MessageUtils.sendString(context.sender, "&cThis command cannot be used by command blocks.");
    }

}
