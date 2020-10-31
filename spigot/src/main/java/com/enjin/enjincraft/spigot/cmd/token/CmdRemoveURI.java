package com.enjin.enjincraft.spigot.cmd.token;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.util.StringUtils;

public class CmdRemoveURI extends EnjCommand {

    public CmdRemoveURI(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("removeuri");
        this.requiredArgs.add("id");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN_CREATE)
                .withAllowedSenderTypes(SenderType.PLAYER)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        String id = context.args().get(0);

        TokenManager tokenManager = bootstrap.getTokenManager();
        TokenModel tokenModel = tokenManager.getToken(id);
        if (tokenModel == null) {
            Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender());
            return;
        } else if (StringUtils.isEmpty(tokenModel.getMetadataURI())) {
            Translation.COMMAND_TOKEN_REMOVEURI_EMPTY.send(context.sender());
            return;
        }

        int result = tokenManager.updateMetadataURI(tokenModel.getId(), null);
        switch (result) {
            case TokenManager.TOKEN_NOSUCHTOKEN:
                Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender());
                break;
            case TokenManager.TOKEN_UPDATE_SUCCESS:
                Translation.COMMAND_TOKEN_REMOVEURI_SUCCESS.send(context.sender());
                break;
            case TokenManager.TOKEN_ISNOTBASE:
                Translation.COMMAND_TOKEN_ISNONFUNGIBLEINSTANCE.send(context.sender());
                break;
            case TokenManager.TOKEN_UPDATE_FAILED:
                Translation.COMMAND_TOKEN_REMOVEURI_FAILED.send(context.sender());
                break;
            default:
                bootstrap.debug(String.format("Unhandled result when removing the URI (status: %d)", result));
                break;
        }
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_REMOVEURI_DESCRIPTION;
    }

}
