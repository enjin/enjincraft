package com.enjin.enjincraft.spigot.cmd.token;

import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.token.TokenManager;
import org.bukkit.command.CommandSender;

public class CmdImport extends EnjCommand {

    public CmdImport(EnjCommand parent) {
        super(parent);
        this.aliases.add("import");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.CONSOLE)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();

        int result = bootstrap.getTokenManager().importTokens();
        switch (result) {
            case TokenManager.TOKEN_IMPORT_SUCCESS:
                Translation.COMMAND_TOKEN_IMPORT_COMPLETE.send(sender);
                Translation.COMMAND_TOKEN_IMPORT_SUCCESS.send(sender);
                break;
            case TokenManager.TOKEN_IMPORT_PARTIAL:
                Translation.COMMAND_TOKEN_IMPORT_COMPLETE.send(sender);
                Translation.COMMAND_TOKEN_IMPORT_PARTIAL.send(sender);
                break;
            case TokenManager.TOKEN_IMPORT_EMPTY:
                Translation.COMMAND_TOKEN_IMPORT_EMPTY.send(sender);
                break;
            case TokenManager.TOKEN_IMPORT_FAILED:
                Translation.COMMAND_TOKEN_IMPORT_FAILED.send(sender);
                break;
            default:
                bootstrap.debug(String.format("Unhandled result when importing token(s) (status: %d)", result));
                break;
        }
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_IMPORT_DESCRIPTION;
    }

}
