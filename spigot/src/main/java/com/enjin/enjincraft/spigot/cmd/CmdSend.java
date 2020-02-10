package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.cmd.arg.PlayerArgumentProcessor;
import com.enjin.enjincraft.spigot.enums.Permission;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.wallet.MutableBalance;
import com.enjin.sdk.TrustedPlatformClient;
import com.enjin.sdk.graphql.GraphQLResponse;
import com.enjin.sdk.model.service.requests.CreateRequest;
import com.enjin.sdk.model.service.requests.Transaction;
import com.enjin.sdk.model.service.requests.data.SendTokenData;
import com.enjin.java_commons.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CmdSend extends EnjCommand {

    public CmdSend(SpigotBootstrap bootstrap, EnjCommand parent) {
        super(bootstrap, parent);
        this.aliases.add("send");
        this.requiredArgs.add("player");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_SEND)
                .build();
    }

    @Override
    public List<String> tab(CommandContext context) {
        if (context.args.size() == 1)
            return PlayerArgumentProcessor.INSTANCE.tab(context.sender, context.args.get(0));
        return new ArrayList<>(0);
    }

    @Override
    public void execute(CommandContext context) {
        Player sender = context.player;
        EnjPlayer senderEnjPlayer = context.enjPlayer;

        if (context.args.isEmpty())
            return;

        if (!senderEnjPlayer.isLinked()) {
            Translation.WALLET_NOTLINKED_SELF.send(sender);
            return;
        }

        Player target = Bukkit.getPlayer(context.args.get(0));
        if (target == null || !target.isOnline()) {
            Translation.ERRORS_PLAYERNOTONLINE.send(sender, context.args.get(0));
            return;
        }

        if (target == sender) {
            Translation.ERRORS_CHOOSEOTHERPLAYER.send(sender);
            return;
        }

        Optional<EnjPlayer> optionalTarget = bootstrap.getPlayerManager().getPlayer(target);
        if (!optionalTarget.isPresent())
            return;
        EnjPlayer targetEnjPlayer = optionalTarget.get();

        if (!targetEnjPlayer.isLinked()) {
            Translation.WALLET_NOTLINKED_OTHER.send(sender, target.getName());
            return;
        }

        ItemStack is = sender.getInventory().getItemInMainHand();

        if (is.getType() == Material.AIR) {
            Translation.COMMAND_SEND_MUSTHOLDITEM.send(sender);
            return;
        }

        String tokenId = TokenUtils.getTokenID(is);

        if (StringUtils.isEmpty(tokenId)) {
            Translation.COMMAND_SEND_ITEMNOTTOKEN.send(sender);
            return;
        }

        MutableBalance balance = senderEnjPlayer.getTokenWallet().getBalance(tokenId);
        balance.deposit(is.getAmount());
        sender.getInventory().clear(sender.getInventory().getHeldItemSlot());

        if (BigInteger.ZERO.equals(senderEnjPlayer.getEnjAllowance())) {
            Translation.WALLET_ALLOWANCENOTSET.send(sender);
            return;
        }

        send(sender, senderEnjPlayer.getIdentityId(), targetEnjPlayer.getIdentityId(),
                tokenId, is.getAmount());
    }

    private void send(Player sender, int senderId, int targetId, String tokenId, int amount) {
        TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
        client.getRequestsService().createRequestAsync(new CreateRequest()
                        .appId(client.getAppId())
                        .identityId(senderId)
                        .sendToken(SendTokenData.builder()
                                .recipientIdentityId(targetId)
                                .tokenId(tokenId)
                                .value(amount)
                                .build()),
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
