package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import org.bukkit.command.CommandSender;

public class CmdHelp extends EnjCommand {

    public CmdHelp(EnjCommand parent) {
        super(parent);
        this.aliases.add("help");
        this.aliases.add("h");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.ANY)
                .withPermission(Permission.CMD_HELP)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        parent.ifPresent(parent -> showHelp(context.sender, parent));
    }

    private void showHelp(CommandSender sender, EnjCommand command) {
        command.showHelp(sender);
        command.subCommands.forEach(c -> showHelp(sender, c));
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_HELP_DESCRIPTION;
    }

}
