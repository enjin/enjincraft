package com.enjin.ecmp.spigot.commands.subcommands;

import com.enjin.ecmp.spigot.BasePlugin;
import com.enjin.ecmp.spigot.player.PlayerManager;
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
import com.enjin.ecmp.spigot.player.EnjinCoinPlayer;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.util.TokenUtils;
import net.kyori.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class SendCommand {

    private BasePlugin plugin;

    public SendCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(Player sender, String[] args) {
        PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
        EnjinCoinPlayer senderMP = playerManager.getPlayer(sender.getUniqueId());

        if (args.length > 0) {
            if (!senderMP.isLinked()) {
                MessageUtils.sendComponent(sender, TextComponent.of("You must link your wallet before using this command."));
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target != null && target != sender) {
                EnjinCoinPlayer targetMP = playerManager.getPlayer(target.getUniqueId());
                if (!targetMP.isLinked()) {
                    MessageUtils.sendComponent(sender, TextComponent.of("That player has not linked a wallet."));
                    return;
                }

                ItemStack is = sender.getInventory().getItemInMainHand();
                if (is == null) {
                    MessageUtils.sendComponent(sender, TextComponent.of("You must be holding a token you wish to send."));
                } else {
                    String tokenId = TokenUtils.getTokenID(is);
                    if (tokenId == null) {
                        MessageUtils.sendComponent(sender, TextComponent.of("You must be holding an Enjin Coin token item."));
                    } else {
                        MutableBalance balance = senderMP.getTokenWallet().getBalance(tokenId);
                        balance.deposit(is.getAmount());
                        sender.getInventory().clear(sender.getInventory().getHeldItemSlot());

                        IdentitiesService service = plugin.getBootstrap().getTrustedPlatformClient().getIdentitiesService();
                        service.getIdentitiesAsync(new GetIdentities().identityId(senderMP.getIdentityId()), response -> {
                            if (response.isSuccess()) {
                                GraphQLResponse<List<Identity>> body = response.body();
                                if (body.isSuccess()) {
                                    List<Identity> data = body.getData();
                                    if (data != null && !data.isEmpty()) {
                                        Identity identity = data.get(0);
                                        BigInteger allowance = identity.getEnjAllowance();

                                        if (allowance == null || allowance.equals(BigInteger.ZERO)) {
                                            MessageUtils.sendComponent(sender, TextComponent.of("Your allowance is not set. Please confirm the request in your wallet app."));
                                        } else {
                                            send(sender, senderMP.getIdentityId(), targetMP.getIdentityId(),
                                                    tokenId, is.getAmount());
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private void send(Player sender, int senderId, int targetId, String tokenId, int amount) {
        RequestsService service = this.plugin.getBootstrap().getTrustedPlatformClient().getRequestsService();
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
