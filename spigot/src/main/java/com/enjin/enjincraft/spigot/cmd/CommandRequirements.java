package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.enums.MessageAction;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
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
            if (!senderAllowed)
                sendInvalidSenderTypeMessage(sender, senderType);
            if (!hasPermission)
                sendNoPermissionMessage(sender);
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
            Translation.COMMAND_API_REQUIREMENTS_INVALIDPLAYER.send(sender);
        else if (senderType == SenderType.CONSOLE)
            Translation.COMMAND_API_REQUIREMENTS_INVALIDCONSOLE.send(sender);
        else if (senderType == SenderType.REMOTE_CONSOLE)
            Translation.COMMAND_API_REQUIREMENTS_INVALIDREMOTE.send(sender);
        else if (senderType == SenderType.BLOCK)
            Translation.COMMAND_API_REQUIREMENTS_INVALIDBLOCK.send(sender);
    }

    protected void sendNoPermissionMessage(CommandSender sender) {
        Translation.COMMAND_API_REQUIREMENTS_NOPERMISSION.send(sender, permission.node());
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
