package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.Messages;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.VeryifyRequirements;
import com.enjin.ecmp.spigot.enums.CommandProcess;
import com.enjin.ecmp.spigot.enums.MessageAction;
import com.enjin.ecmp.spigot.i18n.Translation;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.util.TextUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class EnjCommand {

    protected SpigotBootstrap bootstrap;
    protected Optional<EnjCommand> parent;
    protected List<String> aliases;
    protected List<EnjCommand> subCommands;
    protected List<String> requiredArgs;
    protected List<String> optionalArgs;
    protected CommandRequirements requirements;

    public EnjCommand(SpigotBootstrap bootstrap, EnjCommand parent) {
        this.bootstrap = bootstrap;
        this.parent = Optional.ofNullable(parent);
        this.aliases = new ArrayList<>();
        this.subCommands = new ArrayList<>();
        this.requiredArgs = new ArrayList<>();
        this.optionalArgs = new ArrayList<>();
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.ANY)
                .build();
    }

    public EnjCommand(SpigotBootstrap bootstrap) {
        this(bootstrap, null);
    }

    public abstract void execute(CommandContext context);

    public abstract Translation getUsageTranslation();

    public List<String> tab(CommandContext context) {
        return new ArrayList<>();
    }

    private List<String> tab0(CommandContext context) {
        List<String> tabResults = new ArrayList<>();

        if (!subCommands.isEmpty()) {
            List<String> aliases = subCommands.stream()
                    .filter(c -> c.requirements.areMet(context, MessageAction.OMIT))
                    .map(c -> c.aliases.get(0).toLowerCase())
                    .collect(Collectors.toList());

            if (!context.args.isEmpty()) {
                tabResults.addAll(aliases.stream()
                        .filter(a -> a.startsWith(context.args.get(0).toLowerCase()))
                        .collect(Collectors.toList()));
            }
        } else {
            tabResults.addAll(tab(context));
        }

        return tabResults;
    }

    public void showUsage(CommandSender sender) {
        showUsage(sender, VeryifyRequirements.YES, MessageAction.OMIT);
    }

    public void showUsage(CommandSender sender, VeryifyRequirements verifyRequirements, MessageAction action) {
        if (verifyRequirements == VeryifyRequirements.NO || requirements.areMet(sender, action)) {
            String usage = getUsage();
            if (SenderType.type(sender) != SenderType.PLAYER)
                usage = usage.replaceFirst("/", "");
            MessageUtils.sendString(sender, usage);
        }
    }

    public String getUsage() {
        StringBuilder builder = new StringBuilder();

        builder.append("&6/");

        List<EnjCommand> commandStack = CommandContext.createCommandStackAsList(this);
        for (EnjCommand command : commandStack) {
            builder.append(TextUtil.concat(command.aliases, ",")).append(' ');
        }

        builder.append("&e");

        if (!requiredArgs.isEmpty()) {
            builder.append(TextUtil.concat(requiredArgs.stream()
                    .map(s -> String.format("<%s>", s))
                    .collect(Collectors.toList()), " "))
                    .append(' ');
        }

        if (!optionalArgs.isEmpty()) {
            builder.append(TextUtil.concat(optionalArgs.stream()
                    .map(s -> String.format("[%s]", s))
                    .collect(Collectors.toList()), " "))
                    .append(' ');
        }

        builder.append("&f").append(getUsageTranslation().toString());

        return builder.toString();
    }

    public void process(CommandContext context, CommandProcess process) {
        try {
            if (!isValid(context, process.showErrorMessages() ? MessageAction.SEND : MessageAction.OMIT))
                return;

            if (context.args.size() > 0) {
                for (EnjCommand subCommand : subCommands) {
                    if (!subCommand.aliases.contains(context.args.get(0).toLowerCase()))
                        continue;
                    context.args.remove(0);
                    context.commandStack.push(this);
                    subCommand.process(context, process);
                    return;
                }
            }

            if (process == CommandProcess.EXECUTE) {
                execute(context);
            } else {
                context.tabCompletionResult = tab0(context);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Messages.error(context.sender, ex);
        }

    }

    protected void addSubCommand(EnjCommand subCommand) {
        this.subCommands.add(subCommand);
    }

    private boolean isValid(CommandContext context, MessageAction action) {
        return requirements.areMet(context, action) && validArgs(context);
    }

    private boolean validArgs(CommandContext context) {
        return context.args.size() >= requiredArgs.size();
    }

}
