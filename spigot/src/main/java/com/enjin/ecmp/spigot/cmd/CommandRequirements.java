package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.enums.MessageAction;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.util.MessageUtils;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandRequirements {

    protected List<SenderType> allowedSenderTypes = new ArrayList<>();
    protected Permission permission;

    public boolean areMet(CommandSender sender, MessageAction messageAction) {
        return areMet(sender, SenderType.type(sender), messageAction);
    }

    public boolean areMet(CommandContext context, MessageAction messageAction) {
        return areMet(context.sender, context.senderType, messageAction);
    }

    public boolean areMet(CommandSender sender, SenderType senderType, MessageAction messageAction) {
        boolean senderAllowed = isSenderAllowed(sender);
        boolean hasPermission = hasPermission(sender);

        if (messageAction == MessageAction.SEND) {
            if (!senderAllowed) sendInvalidSenderTypeMessage(sender, senderType);
            if (!hasPermission) sendNoPermissionMessage(sender);
        }

        return senderAllowed && hasPermission;
    }

    protected boolean isSenderAllowed(CommandContext context) {
        return isSenderAllowed(context.senderType);
    }

    protected boolean isSenderAllowed(CommandSender sender) {
        return isSenderAllowed(SenderType.type(sender));
    }

    protected boolean isSenderAllowed(SenderType type) {
        return allowedSenderTypes.contains(SenderType.ANY) || allowedSenderTypes.contains(type);
    }

    protected boolean hasPermission(CommandContext context) {
        return hasPermission(context.sender);
    }

    protected boolean hasPermission(CommandSender sender) {
        return permission == null || permission.hasPermission(sender);
    }

    protected void sendInvalidSenderTypeMessage(CommandSender sender, SenderType senderType) {
        if (senderType == SenderType.PLAYER)
            MessageUtils.sendString(sender, "&cThis command cannot be used by players.");
        else if (senderType == SenderType.CONSOLE)
            MessageUtils.sendString(sender, "&cThis command cannot be used by the console.");
        else if (senderType == SenderType.REMOTE_CONSOLE)
            MessageUtils.sendString(sender, "&cThis command cannot be used remotely.");
        else if (senderType == SenderType.BLOCK)
            MessageUtils.sendString(sender, "&cThis command cannot be used by command blocks.");
    }

    protected void sendNoPermissionMessage(CommandSender sender) {
        MessageUtils.sendString(sender,
                String.format("&cYou do not have the permission required for this command: &6%s",
                        permission.node()));
    }

    public static class Builder {

        private CommandRequirements requirements = new CommandRequirements();

        public Builder() {
            requirements.allowedSenderTypes.add(SenderType.ANY);
        }

        public Builder withPermission(Permission permission) {
            requirements.permission = permission;
            return this;
        }

        public Builder withAllowedSenderTypes(SenderType... types) {
            requirements.allowedSenderTypes.clear();
            if (types != null && types.length != 0)
                requirements.allowedSenderTypes.addAll(Arrays.asList(types));
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
