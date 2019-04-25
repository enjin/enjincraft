package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.Callback;
import com.enjin.enjincoin.sdk.http.Result;
import com.enjin.enjincoin.sdk.model.service.requests.CreateRequest;
import com.enjin.enjincoin.sdk.model.service.requests.CreateRequestResult;
import com.enjin.enjincoin.sdk.model.service.requests.TransactionType;
import com.enjin.enjincoin.sdk.model.service.requests.data.SendTokenData;
import com.enjin.enjincoin.sdk.service.ethereum.EthereumService;
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

public class SendCommand {

    private BasePlugin plugin;

    public SendCommand(BasePlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(Player sender, String[] args) {
        PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
        MinecraftPlayer senderMP = playerManager.getPlayer(sender.getUniqueId());

        if (args.length > 0) {
            if (!senderMP.isLoaded()) {
                MessageUtils.sendMessage(sender, TextComponent.of("You must link your wallet before using this command."));
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target != null && target != sender) {
                MinecraftPlayer targetMP = playerManager.getPlayer(target.getUniqueId());
                if (!targetMP.isLoaded()) {
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

                        EthereumService service = this.plugin.getBootstrap().getSdkController().getClient().getEthereumService();
                        service.getAllowanceAsync(senderMP.getIdentity().getEthereumAddress(), result -> {
                            if (result.isSuccess()) {
                                if (result.body() == null || result.body().equals(BigInteger.ZERO)) {
                                    MessageUtils.sendMessage(sender, TextComponent.of("Your allowance is not set. Please confirm the request in your wallet app."));
                                } else {
                                    send(sender, senderMP.getIdentity().getId(), targetMP.getIdentity().getId(),
                                            tokenId, is.getAmount());
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

    private void send(Player sender, BigInteger senderId, BigInteger targetId, String tokenId, int amount) {
        RequestsService service = this.plugin.getBootstrap().getSdkController().getClient().getRequestsService();
        try {
            Result<GraphQLResponse<CreateRequestResult>> result = service.createRequestSync(new CreateRequest()
                    .withIdentityId(senderId)
                    .withType(TransactionType.SEND)
                    .withSendTokenData(SendTokenData.builder()
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
