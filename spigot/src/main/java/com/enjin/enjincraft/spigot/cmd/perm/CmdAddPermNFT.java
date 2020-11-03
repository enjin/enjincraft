package com.enjin.enjincraft.spigot.cmd.perm;

import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.util.TokenUtils;

import java.util.List;

public class CmdAddPermNFT extends EnjCommand {

    public CmdAddPermNFT(EnjCommand parent) {
        super(parent);
        this.aliases.add("addpermnft");
        this.requiredArgs.add("id");
        this.requiredArgs.add("index");
        this.requiredArgs.add("perm");
        this.optionalArgs.add("worlds...");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN_ADDPERM)
                .withAllowedSenderTypes(SenderType.PLAYER)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        String id = context.args().get(0);
        String index = context.args().get(1);
        String perm = context.args().get(2);
        List<String> worlds = context.args().size() > requiredArgs.size()
                ? context.args().subList(requiredArgs.size(), context.args().size())
                : null;

        TokenManager tokenManager = bootstrap.getTokenManager();

        TokenModel baseModel = tokenManager.getToken(id);
        if (baseModel != null && !baseModel.isNonfungible()) {
            Translation.COMMAND_TOKEN_ISFUNGIBLE.send(context.sender());
            return;
        } else if (baseModel != null) {
            id = baseModel.getId();
        }

        String fullId;
        try {
            index = TokenUtils.parseIndex(index);
            fullId = TokenUtils.createFullId(id, index);
        } catch (IllegalArgumentException e) {
            Translation.COMMAND_TOKEN_INVALIDFULLID.send(context.sender());
            return;
        } catch (Exception e) {
            bootstrap.log(e);
            return;
        }

        boolean isGlobal = worlds == null || worlds.contains(TokenManager.GLOBAL);
        int result = isGlobal
                ? tokenManager.addPermissionToToken(perm, fullId, TokenManager.GLOBAL)
                : tokenManager.addPermissionToToken(perm, fullId, worlds);
        switch (result) {
            case TokenManager.PERM_ADDED_SUCCESS:
                Translation.COMMAND_TOKEN_ADDPERM_PERMADDED.send(context.sender());
                break;
            case TokenManager.TOKEN_NOSUCHTOKEN:
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender());
                break;
            case TokenManager.PERM_ADDED_DUPLICATEPERM:
                Translation.COMMAND_TOKEN_ADDPERM_DUPLICATEPERM.send(context.sender());
                break;
            case TokenManager.PERM_ADDED_BLACKLISTED:
                Translation.COMMAND_TOKEN_ADDPERM_PERMREJECTED.send(context.sender());
                break;
            case TokenManager.PERM_ISGLOBAL:
                Translation.COMMAND_TOKEN_PERM_ISGLOBAL.send(context.sender());
                break;
            case TokenManager.TOKEN_UPDATE_FAILED:
                Translation.COMMAND_TOKEN_UPDATE_FAILED.send(context.sender());
                break;
            default:
                bootstrap.debug(String.format("Unhandled result when adding non-fungible permission (status: %d)", result));
                break;
        }
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_ADDPERMNFT_DESCRIPTION;
    }

}
