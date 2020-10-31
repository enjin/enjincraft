package com.enjin.enjincraft.spigot.cmd.token;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.token.TokenManager;

import java.util.List;

public class CmdRevokePerm extends EnjCommand {

    public CmdRevokePerm(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("revokeperm");
        this.requiredArgs.add("id");
        this.requiredArgs.add("perm");
        this.optionalArgs.add("worlds...");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN_REVOKEPERM)
                .withAllowedSenderTypes(SenderType.PLAYER)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        String id = context.args().get(0);
        String perm = context.args().get(1);
        List<String> worlds = context.args().size() > requiredArgs.size()
                ? context.args().subList(requiredArgs.size(), context.args().size())
                : null;

        boolean isGlobal = worlds == null || worlds.contains(TokenManager.GLOBAL);
        int result = isGlobal
                ? bootstrap.getTokenManager().removePermissionFromToken(perm, id, TokenManager.GLOBAL)
                : bootstrap.getTokenManager().removePermissionFromToken(perm, id, worlds);
        switch (result) {
            case TokenManager.PERM_REMOVED_SUCCESS:
                Translation.COMMAND_TOKEN_REVOKEPERM_PERMREVOKED.send(context.sender());
                break;
            case TokenManager.TOKEN_NOSUCHTOKEN:
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender());
                break;
            case TokenManager.PERM_REMOVED_NOPERMONTOKEN:
                Translation.COMMAND_TOKEN_REVOKEPERM_PERMNOTONTOKEN.send(context.sender());
                break;
            case TokenManager.PERM_ISGLOBAL:
                Translation.COMMAND_TOKEN_PERM_ISGLOBAL.send(context.sender());
                break;
            case TokenManager.TOKEN_UPDATE_FAILED:
                Translation.COMMAND_TOKEN_UPDATE_FAILED.send(context.sender());
                break;
            default:
                bootstrap.debug(String.format("Unhandled result when removing base permission (status: %d)", result));
                break;
        }
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_REVOKEPERM_DESCRIPTION;
    }

}
