package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.Messages;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.Permission;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class EnjCommand {

    public static final CommandRequirements DEFAULT_REQUIREMENTS = CommandRequirements.builder()
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

    public List<String> tab(CommandContext context) {
        return Arrays.asList();
    }

    protected List<String> tab0(CommandContext context) {
        List<String> tabResults = new ArrayList<>();

        if (!subCommands.isEmpty()) {
            if (context.args.isEmpty()) {
                tabResults.addAll(subCommands.stream()
                        .map(c -> c.aliases.get(0))
                        .collect(Collectors.toList()));
            } else {
                tabResults.addAll(subCommands.stream()
                        .filter(c -> c.aliases.get(0).toLowerCase()
                                .startsWith(context.args.get(0).toLowerCase()))
                        .map(c -> c.aliases.get(0)).collect(Collectors.toList()));
            }
        } else {
            tabResults.addAll(tab(context));
        }

        return tabResults;
    }

    public void process(CommandContext context, boolean executing) {
        try {
            if (!requirements.areMet(context, executing)) return;

            if (context.args.size() > 0) {
                for (EnjCommand subCommand : subCommands) {
                    if (!subCommand.aliases.contains(context.args.get(0).toLowerCase())) continue;
                    context.args.remove(0);
                    context.commandStack.push(this);
                    subCommand.process(context, executing);
                    return;
                }
            }

            if (executing) {
                execute(context);
            } else {
                context.tabCompletionResult = tab0(context);
            }
        } catch (
                Exception ex) {
            ex.printStackTrace();
            Messages.error(context.sender, ex);
        }

    }

    protected void addSubCommand(EnjCommand subCommand) {
        this.subCommands.add(subCommand);
    }

}
