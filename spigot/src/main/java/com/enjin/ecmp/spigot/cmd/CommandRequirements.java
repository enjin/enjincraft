package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.cmd.arg.Argument;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.util.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandRequirements {

    protected List<SenderType> allowedSenderTypes = new ArrayList<>();
    protected Permission permission;
    protected List<Argument> arguments = new ArrayList<>();

    public boolean areMet(CommandContext context, boolean informIfNot) {
        boolean senderAllowed = isSenderAllowed(context);
        boolean hasPermission = hasPermission(context);
        boolean validArgs = validArgs(context);

        if (informIfNot) {
            if (!senderAllowed) sendInvalidSenderTypeMessage(context);
            if (!hasPermission) sendNoPermissionMessage(context);
            // TODO: if !validArgs print usage
        }

        return senderAllowed && hasPermission;
    }

    private boolean isSenderAllowed(CommandContext context) {
        return allowedSenderTypes.contains(SenderType.ANY) || allowedSenderTypes.contains(context.senderType);
    }

    private boolean hasPermission(CommandContext context) {
        return permission == null || permission.hasPermission(context.sender);
    }

    private boolean validArgs(CommandContext context) {
        return context.args.size() >= arguments.stream().filter(a -> a.isRequired()).count();
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

    private void sendNoPermissionMessage(CommandContext context) {
        MessageUtils.sendString(context.sender,
                String.format("&cYou do not have the permission required for this command: &6%s",
                        permission.node()));
    }

    public static class Builder {

        private CommandRequirements requirements = new CommandRequirements();

        public Builder withPermission(Permission permission) {
            requirements.permission = permission;
            return this;
        }

        public Builder withAllowedSenderTypes(SenderType... types) {
            requirements.allowedSenderTypes.clear();
            if (types != null) requirements.allowedSenderTypes.addAll(Arrays.asList(types));
            return this;
        }

        public CommandRequirements build() {
            return requirements;
        }

    }

    public static Builder builder() {
        return new Builder();
    }

}
