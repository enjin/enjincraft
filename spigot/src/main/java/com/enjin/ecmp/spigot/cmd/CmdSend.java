package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.GraphQLException;
import com.enjin.ecmp.spigot.Messages;
import com.enjin.ecmp.spigot.NetworkException;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.cmd.arg.PlayerArgument;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.util.TokenUtils;
import com.enjin.ecmp.spigot.wallet.MutableBalance;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpCallback;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.identities.GetIdentities;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.model.service.requests.CreateRequest;
import com.enjin.enjincoin.sdk.model.service.requests.Transaction;
import com.enjin.enjincoin.sdk.model.service.requests.data.SendTokenData;
import com.enjin.enjincoin.sdk.service.identities.IdentitiesService;
import com.enjin.enjincoin.sdk.service.requests.RequestsService;
import com.enjin.java_commons.StringUtils;
import net.kyori.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class CmdSend extends EnjCommand {

    public CmdSend(SpigotBootstrap bootstrap) {
        super(bootstrap);
        this.aliases.add("send");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_SEND)
                .withArguments(PlayerArgument.REQUIRED)
                .build();
    }

    @Override
    public void execute(CommandContext context) {
        Player sender = context.player;
        EnjPlayer senderEnjPlayer = context.enjPlayer;

        // TODO implement argument handling
        if (context.args.size() == 0) return;

        if (!senderEnjPlayer.isLinked()) {
            Messages.identityNotLinked(sender);
            return;
        }

        Player target = Bukkit.getPlayer(context.args.get(0));
        if (target == null) {
            MessageUtils.sendString(sender, String.format("&6%s &cis not online.", context.args.get(0)));
            return;
        }

        if (target == sender) {
            MessageUtils.sendString(sender, "&cYou must specify a player other than yourself.");
            return;
        }

        EnjPlayer targetEnjPlayer = bootstrap.getPlayerManager().getPlayer(target).orElse(null);

        if (!targetEnjPlayer.isLinked()) {
            MessageUtils.sendString(sender, String.format("&6%s &chas not linked a wallet and cannot receive tokens.", target.getName()));
            return;
        }

        ItemStack is = sender.getInventory().getItemInMainHand();

        if (is == null) {
            MessageUtils.sendString(sender, "&cYou must hold the tokenized item you wish to send.");
            return;
        }

        String tokenId = TokenUtils.getTokenID(is);

        if (StringUtils.isEmpty(tokenId)) {
            MessageUtils.sendString(sender, "&cThe held item is not associated with a token.");
            return;
        }

        MutableBalance balance = senderEnjPlayer.getTokenWallet().getBalance(tokenId);
        balance.deposit(is.getAmount());
        sender.getInventory().clear(sender.getInventory().getHeldItemSlot());

        if (BigInteger.ZERO.equals(senderEnjPlayer.getEnjAllowance())) {
            Messages.allowanceNotSet(sender);
            return;
        }

        send(sender, senderEnjPlayer.getIdentityId(), targetEnjPlayer.getIdentityId(),
                tokenId, is.getAmount());
    }

    private void send(Player sender, int senderId, int targetId, String tokenId, int amount) {
        bootstrap.getTrustedPlatformClient()
                .getRequestsService().createRequestAsync(new CreateRequest()
                        .identityId(senderId)
                        .sendToken(SendTokenData.builder()
                                .recipientIdentityId(targetId)
                                .tokenId(tokenId)
                                .value(amount)
                                .build()),
                networkResponse -> {
                    if (!networkResponse.isSuccess()) {
                        NetworkException exception = new NetworkException(networkResponse.code());
                        Messages.error(sender, exception);
                        throw exception;
                    }

                    GraphQLResponse<Transaction> graphQLResponse = networkResponse.body();
                    if (!graphQLResponse.isSuccess()) {
                        GraphQLException exception = new GraphQLException(graphQLResponse.getErrors());
                        Messages.error(sender, exception);
                        throw exception;
                    }

                    MessageUtils.sendString(sender, "&aSend request submitted successfully. Please confirm the request in the Enjin Wallet.");
                });
    }

}
