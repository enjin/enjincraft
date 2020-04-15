package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.arg.PlayerArgumentProcessor;
import com.enjin.enjincraft.spigot.cmd.arg.TokenDefinitionArgumentProcessor;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.sdk.TrustedPlatformClient;
import com.enjin.sdk.graphql.GraphQLResponse;
import com.enjin.sdk.models.request.CreateRequest;
import com.enjin.sdk.models.request.Transaction;
import com.enjin.sdk.models.request.data.SendTokenData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CmdDevSend extends EnjCommand {

    public CmdDevSend(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("devsend");
        this.requiredArgs.add("player");
        this.requiredArgs.add("token-id");
        this.requiredArgs.add("amount");
        this.requirements = CommandRequirements.builder()
                                               .withAllowedSenderTypes(SenderType.CONSOLE)
                                               .build();
    }

    @Override
    public List<String> tab(CommandContext context) {
        if (context.args.size() == 1) {
            return PlayerArgumentProcessor.INSTANCE.tab(context.sender, context.args.get(0));
        }
        if (context.args.size() == 2) {
            return TokenDefinitionArgumentProcessor.INSTANCE.tab(context.sender, context.args.get(1));
        }
        return new ArrayList<>(0);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender             sender           = context.sender;
        Optional<Player>          optionalPlayer   = PlayerArgumentProcessor.INSTANCE.parse(sender,
                                                                                            context.args.get(0));
        Optional<TokenModel> optionalTokenDef = TokenDefinitionArgumentProcessor.INSTANCE.parse(sender,
                                                                                                     context.args.get(1));
        Optional<Integer>         optionalAmount   = context.argToInt(2);

        if (!optionalPlayer.isPresent()) {
            Translation.ERRORS_PLAYERNOTONLINE.send(sender);
            return;
        }

        if (!optionalTokenDef.isPresent()) {
            // TODO: Add Translation
            return;
        }

        if (!optionalAmount.isPresent()) {
            // TODO: Add Translation
            return;
        }

        Player target = optionalPlayer.get();
        if (!target.isOnline()) {
            Translation.ERRORS_PLAYERNOTONLINE.send(sender, context.args.get(0));
            return;
        }

        Optional<EnjPlayer> optionalEnjPlayer = bootstrap.getPlayerManager().getPlayer(target);
        if (!optionalPlayer.isPresent()) { return; }
        EnjPlayer targetEnjPlayer = optionalEnjPlayer.get();

        if (!targetEnjPlayer.isLinked()) {
            Translation.WALLET_NOTLINKED_OTHER.send(sender, target.getName());
            return;
        }

        TokenModel tokenModel = optionalTokenDef.get();
        Integer amount = optionalAmount.get();

        send(sender, bootstrap.getConfig().getDevIdentityId(), targetEnjPlayer.getIdentityId(),
             tokenModel.getId(), amount);
    }

    private void send(CommandSender sender, int senderId, int targetId, String tokenId, int amount) {
        TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
        client.getRequestService().createRequestAsync(new CreateRequest()
                                                              .appId(client.getAppId())
                                                              .identityId(senderId)
                                                              .sendToken(SendTokenData.builder()
                                                                                      .recipientIdentityId(targetId)
                                                                                      .tokenId(tokenId)
                                                                                      .value(amount)
                                                                                      .build()),
                                                      networkResponse -> {
                                                          if (!networkResponse.isSuccess()) {
                                                              NetworkException exception = new NetworkException(
                                                                      networkResponse.code());
                                                              Translation.ERRORS_EXCEPTION.send(sender,
                                                                                                exception.getMessage());
                                                              throw exception;
                                                          }

                                                          GraphQLResponse<Transaction> graphQLResponse = networkResponse
                                                                  .body();
                                                          if (!graphQLResponse.isSuccess()) {
                                                              GraphQLException exception = new GraphQLException(
                                                                      graphQLResponse.getErrors());
                                                              Translation.ERRORS_EXCEPTION.send(sender,
                                                                                                exception.getMessage());
                                                              throw exception;
                                                          }

                                                          Translation.COMMAND_SEND_SUBMITTED.send(sender);
                                                      });
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_DEVSEND_DESCRIPTION;
    }

}
