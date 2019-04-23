package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.model.service.requests.CreateRequest;
import com.enjin.enjincoin.sdk.model.service.requests.TransactionType;
import com.enjin.enjincoin.sdk.model.service.requests.data.SendTokenData;
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
                        RequestsService service = this.plugin.getBootstrap().getSdkController().getClient().getRequestsService();
                        service.createRequestAsync(
                                new CreateRequest()
                                        .withIdentityId(senderMP.getIdentity().getId())
                                        .withType(TransactionType.SEND)
                                        .withSendTokenData(SendTokenData.builder()
                                                .recipientIdentityId(targetMP.getIdentity().getId())
                                                .tokenId(tokenId)
                                                .value(is.getAmount())
                                                .build()),
                                result -> {
                                    if (result.isSuccess()) {
                                        MessageUtils.sendMessage(sender, TextComponent.of("Please confirm the transaction in your wallet."));
                                    } else {
                                        // todo
                                        MessageUtils.sendMessage(sender, TextComponent.of("Woops, something went wrong."));
                                    }
                                }
                        );
                    }
                }
            }
        } else {
            // TODO invalid arguments
        }
    }

}
