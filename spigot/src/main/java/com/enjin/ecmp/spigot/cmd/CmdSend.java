package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.util.TokenUtils;
import com.enjin.ecmp.spigot.wallet.MutableBalance;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
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
        setAllowedSenderTypes(SenderType.PLAYER);
        this.aliases.add("send");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.enjPlayer == null) return;

        Player sender = context.player;
        EnjPlayer senderEnjPlayer = context.enjPlayer;

        if (context.args.size() == 0) return;

        if (!senderEnjPlayer.isLinked()) {
            MessageUtils.sendString(sender, "&cYou must link your wallet before using this command.");
            return;
        }

        Player target = Bukkit.getPlayer(context.args.get(0));
        if (target == null) {
            MessageUtils.sendString(sender, "&cThat player is not online.");
            return;
        }

        if (target == sender) {
            MessageUtils.sendString(sender, "&cYou cannot send a token to yourself.");
            return;
        }

        EnjPlayer targetEnjPlayer = bootstrap.getPlayerManager().getPlayer(target);

        if (!targetEnjPlayer.isLinked()) {
            MessageUtils.sendString(sender, "&cThat player has not linked a wallet.");
            return;
        }

        ItemStack is = sender.getInventory().getItemInMainHand();

        if (is == null) {
            MessageUtils.sendString(sender, "&cYou must be holding a token you wish to send.");
            return;
        }

        String tokenId = TokenUtils.getTokenID(is);

        if (StringUtils.isEmpty(tokenId)) {
            MessageUtils.sendString(sender, "&cThe item you are holding is not a tokenized item.");
            return;
        }

        MutableBalance balance = senderEnjPlayer.getTokenWallet().getBalance(tokenId);
        balance.deposit(is.getAmount());
        sender.getInventory().clear(sender.getInventory().getHeldItemSlot());

        IdentitiesService service = bootstrap.getTrustedPlatformClient().getIdentitiesService();
        service.getIdentitiesAsync(new GetIdentities().identityId(senderEnjPlayer.getIdentityId()), networkResponse -> {
            if (!networkResponse.isSuccess()) return;

            GraphQLResponse<List<Identity>> graphQLResponse = networkResponse.body();
            if (!graphQLResponse.isSuccess()) return;

            List<Identity> data = graphQLResponse.getData();
            if (data == null || data.isEmpty()) return;

            Identity identity = data.get(0);
            BigInteger allowance = identity.getEnjAllowance();

            if (allowance == null || allowance.equals(BigInteger.ZERO)) {
                MessageUtils.sendString(sender, "&cYour allowance is not set. Please confirm the request in your wallet app.");
                return;
            }

            send(sender, senderEnjPlayer.getIdentityId(), targetEnjPlayer.getIdentityId(),
                    tokenId, is.getAmount());
        });
    }

    private void send(Player sender, int senderId, int targetId, String tokenId, int amount) {
        RequestsService service = bootstrap.getTrustedPlatformClient().getRequestsService();
        try {
            HttpResponse<GraphQLResponse<Transaction>> result = service.createRequestSync(new CreateRequest()
                    .identityId(senderId)
                    .sendToken(SendTokenData.builder()
                            .recipientIdentityId(targetId)
                            .tokenId(tokenId)
                            .value(amount)
                            .build()));

            if (result.isSuccess()) {
                MessageUtils.sendComponent(sender, TextComponent.of("Please confirm the transaction in your wallet."));
            } else {
                MessageUtils.sendComponent(sender, TextComponent.of("Woops, something went wrong."));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
