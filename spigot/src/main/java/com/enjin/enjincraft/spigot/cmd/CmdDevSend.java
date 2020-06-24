package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.arg.PlayerArgumentProcessor;
import com.enjin.enjincraft.spigot.cmd.arg.TokenDefinitionArgumentProcessor;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.PlayerUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
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

    public static final int    ETH_ADDRESS_LENGTH = 42;
    public static final String ETH_ADDRESS_PREFIX = "0x";

    public CmdDevSend(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("devsend");
        this.requiredArgs.add("player");
        this.requiredArgs.add("id");
        this.requiredArgs.add("index|amount");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.CONSOLE)
                .build();
    }

    @Override
    public List<String> tab(CommandContext context) {
        if (context.args.size() == 1)
            return PlayerArgumentProcessor.INSTANCE.tab(context.sender, context.args.get(0));
        else if (context.args.size() == 2)
            return TokenDefinitionArgumentProcessor.INSTANCE.tab(context.sender, context.args.get(1));

        return new ArrayList<>(0);
    }

    @Override
    public void execute(CommandContext context) {
        if (context.args.size() != requiredArgs.size())
            return;

        CommandSender sender    = context.sender;
        String        recipient = context.args.get(0);
        String        id        = context.args.get(1);

        TokenModel tokenModel = TokenDefinitionArgumentProcessor.INSTANCE
                .parse(sender, id)
                .orElse(null);
        if (tokenModel == null) {
            Translation.COMMAND_DEVSEND_INVALIDTOKEN.send(sender);
            return;
        }

        // Process target address
        String targetAddr;
        if (recipient.startsWith(ETH_ADDRESS_PREFIX) && recipient.length() == ETH_ADDRESS_LENGTH) {
            targetAddr = recipient;
        } else if (PlayerUtils.isValidUserName(recipient)) {
            Player targetPlayer = PlayerArgumentProcessor.INSTANCE
                    .parse(sender, recipient)
                    .orElse(null);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                Translation.ERRORS_PLAYERNOTONLINE.send(sender, recipient);
                return;
            }

            EnjPlayer targetEnjPlayer = bootstrap.getPlayerManager()
                    .getPlayer(targetPlayer)
                    .orElse(null);
            if (targetEnjPlayer == null) {
                Translation.ERRORS_PLAYERNOTREGISTERED.send(sender, recipient);
                return;
            } else if (!targetEnjPlayer.isLinked()) {
                Translation.WALLET_NOTLINKED_OTHER.send(sender, targetPlayer.getName());
                return;
            }

            targetAddr = targetEnjPlayer.getEthereumAddress();
        } else {
            Translation.ERRORS_INVALIDPLAYERNAME.send(sender, recipient);
            return;
        }

        // Process send data
        SendTokenData data;
        if (tokenModel.isNonfungible()) { // Non-fungible token
            try {
                String index = context.args.get(2);
                if (index.startsWith("x") || index.startsWith("X")) {
                    index = index.substring(1);
                } else {
                    long parsedLong = Long.parseLong(index);
                    if (parsedLong < 1L)
                        throw new IllegalArgumentException("Provided index is not positive");

                    index = Long.toHexString(parsedLong);
                }

                index = TokenUtils.formatIndex(index);
                if (index.equals(TokenUtils.BASE_INDEX))
                    throw new IllegalArgumentException("Index may not be the base index");

                data = SendTokenData.builder()
                        .recipientAddress(targetAddr)
                        .tokenId(tokenModel.getId())
                        .tokenIndex(index)
                        .build();
            } catch (IllegalArgumentException e) {
                Translation.COMMAND_TOKEN_INVALIDFULLID.send(sender);
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }
        } else { // Fungible token
            try {
                Integer amount = context.argToInt(2).orElse(null);
                if (amount == null || amount <= 0)
                    throw new IllegalArgumentException("Invalid amount to send");

                data = SendTokenData.builder()
                        .recipientAddress(targetAddr)
                        .tokenId(tokenModel.getId())
                        .value(amount)
                        .build();
            } catch (IllegalArgumentException e) {
                Translation.COMMAND_DEVSEND_INVALIDAMOUNT.send(sender);
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }
        }

        send(sender, bootstrap.getConfig().getDevIdentityId(), data);
    }

    private void send(CommandSender sender, int senderId, SendTokenData data) {
        TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
        client.getRequestService().createRequestAsync(new CreateRequest()
                        .appId(client.getAppId())
                        .identityId(senderId)
                        .sendToken(data),
                networkResponse -> {
                    if (!networkResponse.isSuccess()) {
                        NetworkException exception = new NetworkException(networkResponse.code());
                        Translation.ERRORS_EXCEPTION.send(sender, exception.getMessage());
                        throw exception;
                    }

                    GraphQLResponse<Transaction> graphQLResponse = networkResponse.body();
                    if (!graphQLResponse.isSuccess()) {
                        GraphQLException exception = new GraphQLException(graphQLResponse.getErrors());
                        Translation.ERRORS_EXCEPTION.send(sender, exception.getMessage());
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
