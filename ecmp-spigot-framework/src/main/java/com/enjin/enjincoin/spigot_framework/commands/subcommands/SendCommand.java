package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.identities.GetIdentities;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.model.service.requests.CreateRequest;
import com.enjin.enjincoin.sdk.model.service.requests.Transaction;
import com.enjin.enjincoin.sdk.model.service.requests.data.SendTokenData;
import com.enjin.enjincoin.sdk.service.identities.IdentitiesService;
import com.enjin.enjincoin.sdk.service.requests.RequestsService;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.player.PlayerManager;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import com.enjin.enjincoin.spigot_framework.util.TokenUtils;
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
        MinecraftPlayer senderMP = playerManager.getPlayer(sender.getUniqueId());

        if (args.length > 0) {
            if (!senderMP.isLinked()) {
                MessageUtils.sendMessage(sender, TextComponent.of("You must link your wallet before using this command."));
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target != null && target != sender) {
                MinecraftPlayer targetMP = playerManager.getPlayer(target.getUniqueId());
                if (!targetMP.isLinked()) {
                    MessageUtils.sendMessage(sender, TextComponent.of("That player has not linked a wallet."));
                    return;
                }

                ItemStack is = sender.getInventory().getItemInMainHand();
                if (is == null) {
                    MessageUtils.sendMessage(sender, TextComponent.of("You must be holding a token you wish to send."));
                } else {
                    String tokenId = TokenUtils.getTokenID(is);
                    if (tokenId == null) {
                        MessageUtils.sendMessage(sender, TextComponent.of("You must be holding an Enjin Coin token item."));
                    } else {
                        sender.getInventory().clear(sender.getInventory().getHeldItemSlot());
                        senderMP.getWallet().getCheckoutManager().returnItem(is);

                        IdentitiesService service = plugin.getBootstrap().getSdkController().getClient().getIdentitiesService();
                        service.getIdentitiesAsync(new GetIdentities().identityId(senderMP.getIdentity().getId()), response -> {
                            if (response.isSuccess()) {
                                GraphQLResponse<List<Identity>> body = response.body();
                                if (body.isSuccess()) {
                                    List<Identity> data = body.getData();
                                    if (data != null && !data.isEmpty()) {
                                        Identity identity = data.get(0);
                                        BigInteger allowance = identity.getEnjAllowance();

                                        if (allowance == null || allowance.equals(BigInteger.ZERO)) {
                                            MessageUtils.sendMessage(sender, TextComponent.of("Your allowance is not set. Please confirm the request in your wallet app."));
                                        } else {
                                            send(sender, senderMP.getIdentity().getId(), targetMP.getIdentity().getId(),
                                                    tokenId, is.getAmount());
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            }
        } else {
            // TODO invalid arguments
        }
    }

    private void send(Player sender, int senderId, int targetId, String tokenId, int amount) {
        RequestsService service = this.plugin.getBootstrap().getSdkController().getClient().getRequestsService();
        try {
            HttpResponse<GraphQLResponse<Transaction>> result = service.createRequestSync(new CreateRequest()
                    .identityId(senderId)
                    .sendToken(SendTokenData.builder()
                            .recipientIdentityId(targetId)
                            .tokenId(tokenId)
                            .value(amount)
                            .build()));

            if (result.isSuccess()) {
                MessageUtils.sendMessage(sender, TextComponent.of("Please confirm the transaction in your wallet."));
            } else {
                // todo
                MessageUtils.sendMessage(sender, TextComponent.of("Woops, something went wrong."));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
