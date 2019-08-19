package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.Messages;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.Permission;

import java.util.ArrayList;
import java.util.List;

public abstract class EnjCommand {

    private static final CommandRequirements DEFAULT_REQUIREMENTS = CommandRequirements.builder()
            .withAllowedSenderTypes(SenderType.ANY)
            .withPermission(Permission.CMD_ENJ)
            .build();

    protected SpigotBootstrap bootstrap;
    protected List<String> aliases;
    protected List<EnjCommand> subCommands;
    protected CommandRequirements requirements;

    public EnjCommand(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        this.aliases = new ArrayList<>();
        this.subCommands = new ArrayList<>();
        this.requirements = DEFAULT_REQUIREMENTS;
    }

    public abstract void execute(CommandContext context);

    public void process(CommandContext context) {
        try {
            if (!requirements.areMet(context, true)) return;

            if (context.args.size() > 0) {
                for (EnjCommand subCommand : subCommands) {
                    if (subCommand.aliases.contains(context.args.get(0).toLowerCase())) {
                        context.args.remove(0);
                        context.commandStack.push(this);
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

    protected void addSubCommand(EnjCommand subCommand) {
        this.subCommands.add(subCommand);
    }

}
