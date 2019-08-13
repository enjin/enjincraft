package com.enjin.ecmp.spigot.cmd;

import java.util.ArrayList;
import java.util.List;

public abstract class EnjCommand {

    protected List<String> aliases;
    protected List<EnjCommand> subCommands;

    public EnjCommand() {
        this.aliases = new ArrayList<>();
        this.subCommands = new ArrayList<>();
    }

    public abstract void execute(CommandContext context);

    public void process(CommandContext context) {
        if (context.args.size() > 0) {
            for (EnjCommand subCommand : subCommands) {
                if (subCommand.aliases.contains(context.args.get(0).toLowerCase())) {
                    context.args.remove(0);
                    context.commandStack.add(this);
                    subCommand.execute(context);
                    return;
                }
            }
        }

        execute(context);
    }

    protected void addSubCommand(EnjCommand subCommand) {
        this.subCommands.add(subCommand);
    }

}
