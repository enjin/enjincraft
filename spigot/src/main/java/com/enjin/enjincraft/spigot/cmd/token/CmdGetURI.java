package com.enjin.enjincraft.spigot.cmd.token;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
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
import com.enjin.sdk.TrustedPlatformClient;
import com.enjin.sdk.graphql.GraphQLResponse;
import com.enjin.sdk.models.token.GetToken;
import com.enjin.sdk.models.token.Token;
import org.bukkit.command.CommandSender;

public class CmdGetURI extends EnjCommand {

    public CmdGetURI(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("geturi");
        this.requiredArgs.add("id");
        this.requirements = CommandRequirements.builder()
                .withPermission(Permission.CMD_TOKEN_CREATE)
                .withAllowedSenderTypes(SenderType.PLAYER)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        String id = context.args().get(0);

        TokenModel tokenModel = bootstrap.getTokenManager().getToken(id);
        if (tokenModel == null) {
            Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(context.sender());
            return;
        }

        getURI(context.sender(), tokenModel.getId());
    }

    private void getURI(CommandSender sender, String tokenId) {
        TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
        client.getTokenService().getTokenAsync(new GetToken()
                        .tokenId(tokenId)
                        .withItemUri(),
                networkResponse -> {
                    if (!networkResponse.isSuccess()) {
                        NetworkException exception = new NetworkException(networkResponse.code());
                        Translation.ERRORS_EXCEPTION.send(sender, exception.getMessage());
                        throw exception;
                    }

                    GraphQLResponse<Token> graphQLResponse = networkResponse.body();
                    if (!graphQLResponse.isSuccess()) {
                        GraphQLException exception = new GraphQLException(graphQLResponse.getErrors());
                        Translation.ERRORS_EXCEPTION.send(sender, exception);
                        throw exception;
                    }

                    String metadataURI = graphQLResponse.getData().getItemURI();
                    if (StringUtils.isEmpty(metadataURI)) {
                        Translation.COMMAND_TOKEN_GETURI_EMPTY_1.send(sender);
                        Translation.COMMAND_TOKEN_GETURI_EMPTY_2.send(sender);
                        return;
                    }

                    int result = bootstrap.getTokenManager().updateMetadataURI(tokenId, metadataURI);
                    switch (result) {
                        case TokenManager.TOKEN_NOSUCHTOKEN:
                            Translation.COMMAND_TOKEN_NOSUCHTOKEN.send(sender);
                            break;
                        case TokenManager.TOKEN_UPDATE_SUCCESS:
                            Translation.COMMAND_TOKEN_GETURI_SUCCESS.send(sender);
                            break;
                        case TokenManager.TOKEN_ISNOTBASE:
                            Translation.COMMAND_TOKEN_ISNONFUNGIBLEINSTANCE.send(sender);
                            break;
                        case TokenManager.TOKEN_UPDATE_FAILED:
                            Translation.COMMAND_TOKEN_GETURI_FAILED.send(sender);
                            break;
                        default:
                            bootstrap.debug(String.format("Unhandled result when getting the URI (status: %d)", result));
                            break;
                    }
                });
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_TOKEN_GETURI_DESCRIPTION;
    }

}
