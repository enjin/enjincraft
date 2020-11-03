package com.enjin.enjincraft.spigot.cmd.token;

import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.util.TokenUtils;

public class CmdNickname extends EnjCommand {

    public CmdNickname(EnjCommand parent) {
        super(parent);
        this.aliases.add("nickname");
        this.requiredArgs.add("token-id|alt-id");
        this.requiredArgs.add("new-alt-id");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN_CREATE)
                .withAllowedSenderTypes(SenderType.PLAYER)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        String id = context.args().get(0);
        String alternateId = context.args().get(1);

        TokenManager tokenManager = bootstrap.getTokenManager();

        TokenModel tokenModel = tokenManager.getToken(id);
        if (tokenModel == null) {
            Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender());
            return;
        } else if (tokenModel.isNonfungible()) {
            id = TokenUtils.normalizeFullId(tokenModel.getFullId());
        } else {
            id = tokenModel.getFullId();
        }

        int result = tokenManager.updateAlternateId(id, alternateId);
        switch (result) {
            case TokenManager.TOKEN_UPDATE_SUCCESS:
                Translation.COMMAND_TOKEN_NICKNAME_SUCCESS.send(context.sender());
                break;
            case TokenManager.TOKEN_NOSUCHTOKEN:
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender());
                break;
            case TokenManager.TOKEN_DUPLICATENICKNAME:
                Translation.COMMAND_TOKEN_NICKNAME_DUPLICATE.send(context.sender());
                break;
            case TokenManager.TOKEN_HASNICKNAME:
                Translation.COMMAND_TOKEN_NICKNAME_HAS.send(context.sender());
                break;
            case TokenManager.TOKEN_INVALIDNICKNAME:
                Translation.COMMAND_TOKEN_NICKNAME_INVALID.send(context.sender());
                break;
            case TokenManager.TOKEN_ISNOTBASE:
                Translation.COMMAND_TOKEN_ISNONFUNGIBLEINSTANCE.send(context.sender());
                break;
            case TokenManager.TOKEN_UPDATE_FAILED:
                Translation.COMMAND_TOKEN_UPDATE_FAILED.send(context.sender());
                break;
            default:
                bootstrap.debug(String.format("Unhandled result when setting nickname (status: %d)", result));
                break;
        }
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_NICKNAME_DESCRIPTION;
    }
}
