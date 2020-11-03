package com.enjin.enjincraft.spigot.cmd.wallet.send;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.cmd.CommandContext;
import com.enjin.enjincraft.spigot.cmd.CommandRequirements;
import com.enjin.enjincraft.spigot.cmd.EnjCommand;
import com.enjin.enjincraft.spigot.cmd.SenderType;
import com.enjin.enjincraft.spigot.cmd.arg.PlayerArgumentProcessor;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.wallet.MutableBalance;
import com.enjin.sdk.TrustedPlatformClient;
import com.enjin.sdk.graphql.GraphQLResponse;
import com.enjin.sdk.models.request.CreateRequest;
import com.enjin.sdk.models.request.Transaction;
import com.enjin.sdk.models.request.data.SendTokenData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CmdSend extends EnjCommand {

    public CmdSend(EnjCommand parent) {
        super(parent);
        this.aliases.add("send");
        this.requiredArgs.add("player");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_SEND)
                .build();
    }

    @Override
    public List<String> tab(CommandContext context) {
        if (context.args().size() == 1)
            return PlayerArgumentProcessor.INSTANCE.tab(context.sender(), context.args().get(0));

        return new ArrayList<>(0);
    }

    @Override
    public void execute(CommandContext context) {
        Player sender = Objects.requireNonNull(context.player());
        String target = context.args().get(0);

        EnjPlayer senderEnjPlayer = getValidSenderEnjPlayer(context);
        if (senderEnjPlayer == null)
            return;

        Player targetPlayer = getValidTargetPlayer(context, target);
        if (targetPlayer == null)
            return;

        EnjPlayer targetEnjPlayer = getValidTargetEnjPlayer(context, targetPlayer);
        if (targetEnjPlayer == null)
            return;

        ItemStack is = sender.getInventory().getItemInMainHand();
        if (is.getType() == Material.AIR || !is.getType().isItem()) {
            Translation.COMMAND_SEND_MUSTHOLDITEM.send(sender);
            return;
        } else if (!TokenUtils.isValidTokenItem(is)) {
            Translation.COMMAND_SEND_ITEMNOTTOKEN.send(sender);
            return;
        }

        String tokenId    = TokenUtils.getTokenID(is);
        String tokenIndex = TokenUtils.getTokenIndex(is);

        MutableBalance balance = senderEnjPlayer.getTokenWallet().getBalance(tokenId, tokenIndex);
        if (balance == null || balance.balance() == 0) {
            Translation.COMMAND_SEND_DOESNOTHAVETOKEN.send(sender);
            return;
        }

        balance.deposit(is.getAmount());
        sender.getInventory().clear(sender.getInventory().getHeldItemSlot());

        if (BigInteger.ZERO.equals(senderEnjPlayer.getEnjAllowance())) {
            Translation.WALLET_ALLOWANCENOTSET.send(sender);
            return;
        }

        send(sender, senderEnjPlayer.getIdentityId(), targetEnjPlayer.getIdentityId(), tokenId, tokenIndex, is.getAmount());
    }

    private void send(Player sender, int senderId, int targetId, String tokenId, String tokenIndex, int amount) {
        SendTokenData sendTokenData = tokenIndex == null
                ? SendTokenData.builder()
                               .recipientIdentityId(targetId)
                               .tokenId(tokenId)
                               .value(amount)
                               .build()
                : SendTokenData.builder()
                               .recipientIdentityId(targetId)
                               .tokenId(tokenId)
                               .tokenIndex(tokenIndex)
                               .value(amount)
                               .build();

        TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
        client.getRequestService().createRequestAsync(new CreateRequest()
                        .appId(client.getAppId())
                        .identityId(senderId)
                        .sendToken(sendTokenData),
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
        return Translation.COMMAND_SEND_DESCRIPTION;
    }

}
