package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.enums.Usage;
import com.enjin.enjincraft.spigot.enums.VeryifyRequirements;
import com.enjin.enjincraft.spigot.enums.CommandProcess;
import com.enjin.enjincraft.spigot.enums.MessageAction;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.MessageUtils;
import com.enjin.enjincraft.spigot.util.TextUtil;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class EnjCommand {

    protected SpigotBootstrap bootstrap;
    protected EnjCommand parent;
    protected List<String> aliases;
    protected List<EnjCommand> subCommands;
    protected List<String> requiredArgs;
    protected List<String> optionalArgs;
    protected CommandRequirements requirements;

    public EnjCommand(SpigotBootstrap bootstrap, EnjCommand parent) {
        this.bootstrap = bootstrap;
        this.parent = parent;
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
            List<String> als = subCommands.stream()
                    .filter(c -> c.requirements.areMet(context, MessageAction.OMIT))
                    .map(c -> c.aliases.get(0).toLowerCase())
                    .collect(Collectors.toList());

            if (!context.args.isEmpty()) {
                tabResults.addAll(als.stream()
                        .filter(a -> a.startsWith(context.args.get(0).toLowerCase()))
                        .collect(Collectors.toList()));
            }
        } else {
            tabResults.addAll(tab(context));
        }

        return tabResults;
    }

    public void showHelp(CommandSender sender) {
        showHelp(sender, VeryifyRequirements.YES, Usage.ALL);
    }

    public void showHelp(CommandSender sender, VeryifyRequirements verifyRequirements, Usage usage) {
        if (verifyRequirements == VeryifyRequirements.YES && !requirements.areMet(sender, MessageAction.OMIT))
            return;

        MessageUtils.sendString(sender, getUsage(SenderType.type(sender), usage));
    }

    public String getUsage(SenderType type, Usage usage) {
        String output = buildUsage(type, usage);
        if (type != SenderType.PLAYER)
            output = output.replaceFirst("/", "");

        return output;
    }

    public String buildUsage(SenderType type, Usage usage) {
        StringBuilder builder = new StringBuilder();

        builder.append("&6/");

        List<EnjCommand> commandStack = CommandContext.createCommandStackAsList(this);
        for (int i = 0; i < commandStack.size(); i++) {
            EnjCommand command = commandStack.get(i);

            if (i > 0)
                builder.append(' ');

            builder.append(TextUtil.concat(command.aliases, ","));
        }

        builder.append("&e");

        if (!requiredArgs.isEmpty()) {
            builder.append(' ')
                    .append(TextUtil.concat(requiredArgs.stream()
                            .map(s -> String.format("<%s>", s))
                            .collect(Collectors.toList()), " "));
        }

        if (!optionalArgs.isEmpty()) {
            builder.append(' ')
                    .append(TextUtil.concat(optionalArgs.stream()
                            .map(s -> String.format("[%s]", s))
                            .collect(Collectors.toList()), " "));
        }

        if (usage == Usage.ALL)
            builder.append(" &f").append(TextUtil.colorize(getUsageTranslation().translation(type)));

        return builder.toString();
    }

    public void process(CommandContext context, CommandProcess process) {
        try {
            if (!isValid(context, process.getMessageAction()))
                return;

            if (!context.args.isEmpty()) {
                for (EnjCommand subCommand : subCommands) {
                    if (!subCommand.aliases.contains(context.args.get(0).toLowerCase()))
                        continue;

                    context.args.remove(0);
                    context.commandStack.push(this);
                    subCommand.process(context, process);
                    return;
                }
            }

            if (process == CommandProcess.EXECUTE)
                execute(context);
            else
                context.tabCompletionResult = tab0(context);
        } catch (Exception ex) {
            bootstrap.log(ex);
            Translation.ERRORS_EXCEPTION.send(context.sender, ex.getMessage());
        }
    }

    protected void addSubCommand(EnjCommand subCommand) {
        this.subCommands.add(subCommand);
    }

    private boolean isValid(CommandContext context, MessageAction action) {
        return requirements.areMet(context, action) && validArgs(context, action);
    }

    private boolean validArgs(CommandContext context, MessageAction action) {
        boolean result = context.args.size() >= requiredArgs.size();

        if (action == MessageAction.SEND && !result) {
            Translation.COMMAND_API_BADUSAGE.send(context.sender);
            Translation.COMMAND_API_USAGE.send(context.sender, getUsage(context.senderType, Usage.COMMAND_ONLY));
        }

        return result;
    }

    protected EnjPlayer getValidSenderEnjPlayer(@NonNull CommandContext context) throws NullPointerException {
        Player sender = Objects.requireNonNull(context.player, "Expected context to have non-null player as sender");

        EnjPlayer senderEnjPlayer = context.enjPlayer;
        if (senderEnjPlayer == null) {
            Translation.ERRORS_PLAYERNOTREGISTERED.send(sender, sender.getName());
            return null;
        } else if (!senderEnjPlayer.isLinked()) {
            Translation.WALLET_NOTLINKED_SELF.send(sender);
            return null;
        }

        return senderEnjPlayer;
    }

    protected Player getValidTargetPlayer(@NonNull CommandContext context, @NonNull String targetName) {
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            Translation.ERRORS_PLAYERNOTONLINE.send(context.sender, targetName);
            return null;
        } else if (context.player != null && context.player == targetPlayer) {
            Translation.ERRORS_CHOOSEOTHERPLAYER.send(context.sender);
            return null;
        }

        return targetPlayer;
    }

    protected EnjPlayer getValidTargetEnjPlayer(@NonNull CommandContext context,
                                                @NonNull Player targetPlayer) throws NullPointerException {
        EnjPlayer targetEnjPlayer = bootstrap.getPlayerManager()
                .getPlayer(targetPlayer);
        if (targetEnjPlayer == null) {
            Translation.ERRORS_PLAYERNOTREGISTERED.send(context.sender, targetPlayer.getName());
            return null;
        } else if (!targetEnjPlayer.isLinked()) {
            Translation.WALLET_NOTLINKED_OTHER.send(context.sender, targetPlayer.getName());
            return null;
        }

        return targetEnjPlayer;
    }

}
