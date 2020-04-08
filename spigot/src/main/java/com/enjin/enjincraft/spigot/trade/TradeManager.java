package com.enjin.enjincraft.spigot.trade;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.enums.Trader;
import com.enjin.enjincraft.spigot.events.EnjPlayerQuitEvent;
import com.enjin.enjincraft.spigot.i18n.Translation;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.StringUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.sdk.TrustedPlatformClient;
import com.enjin.sdk.graphql.GraphQLResponse;
import com.enjin.sdk.models.request.CreateRequest;
import com.enjin.sdk.models.request.Transaction;
import com.enjin.sdk.models.request.data.CompleteTradeData;
import com.enjin.sdk.models.request.data.CreateTradeData;
import com.enjin.sdk.models.request.data.TokenValueData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class TradeManager implements Listener {

    private SpigotBootstrap bootstrap;

    public TradeManager(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public boolean inviteExists(EnjPlayer sender, EnjPlayer target) {
        return sender.getSentTradeInvites().contains(target);
    }

    public boolean addInvite(EnjPlayer sender, EnjPlayer target) {
        boolean result = !inviteExists(sender, target);

        if (result) {
            sender.getSentTradeInvites().add(target);
            target.getReceivedTradeInvites().add(sender);
        }

        return result;
    }

    public boolean acceptInvite(EnjPlayer inviter, EnjPlayer invited) {
        boolean result = inviteExists(inviter, invited);

        if (result) {
            inviter.getSentTradeInvites().remove(invited);
            invited.getReceivedTradeInvites().remove(invited);

            inviter.setActiveTradeView(new TradeView(bootstrap, inviter, invited, Trader.INVITER));
            invited.setActiveTradeView(new TradeView(bootstrap, invited, inviter, Trader.INVITED));

            inviter.getActiveTradeView().open();
            invited.getActiveTradeView().open();
        }

        return result;
    }

    public boolean declineInvite(EnjPlayer sender, EnjPlayer target) {
        sender.getSentTradeInvites().remove(target);
        return target.getReceivedTradeInvites().remove(sender);
    }

    public void completeTrade(Integer requestId) {
        try {
            TradeSession session = bootstrap.db().getSessionFromRequestId(requestId);
            completeTrade(session);
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    public void completeTrade(TradeSession session) {
        try {
            if (session == null)
                return;

            Optional<Player> inviter = Optional.ofNullable(Bukkit.getPlayer(session.getInviterUuid()));
            Optional<Player> invited = Optional.ofNullable(Bukkit.getPlayer(session.getInvitedUuid()));

            inviter.ifPresent(Translation.COMMAND_TRADE_COMPLETE::send);
            invited.ifPresent(Translation.COMMAND_TRADE_COMPLETE::send);

            bootstrap.db().tradeExecuted(session.getCompleteRequestId());
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    public void sendCompleteRequest(Integer requestId, String tradeId) {
        try {
            TradeSession session = bootstrap.db().getSessionFromRequestId(requestId);
            sendCompleteRequest(session, tradeId);
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    public void sendCompleteRequest(TradeSession session, String tradeId) {
        try {
            if (session == null || StringUtils.isEmpty(tradeId))
                return;

            Optional<Player> inviter = Optional.ofNullable(Bukkit.getPlayer(session.getInviterUuid()));
            Optional<Player> invited = Optional.ofNullable(Bukkit.getPlayer(session.getInvitedUuid()));

            TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
            client.getRequestService().createRequestAsync(new CreateRequest()
                    .appId(client.getAppId())
                    .identityId(session.getInvitedIdentityId())
                    .completeTrade(CompleteTradeData.builder()
                                                    .tradeId(tradeId)
                                                    .build()),
                    networkResponse -> {
                        if (!networkResponse.isSuccess())
                            throw new NetworkException(networkResponse.code());

                        GraphQLResponse<Transaction> graphQLResponse = networkResponse.body();
                        if (!graphQLResponse.isSuccess())
                            throw new GraphQLException(graphQLResponse.getErrors());

                        Transaction dataIn = graphQLResponse.getData();
                        inviter.ifPresent(Translation.COMMAND_TRADE_CONFIRM_WAIT::send);
                        invited.ifPresent(Translation.COMMAND_TRADE_CONFIRM_ACTION::send);

                        try {
                            bootstrap.db().completeTrade(session.getCreateRequestId(),
                                    dataIn.getId(),
                                    tradeId);
                        } catch (SQLException ex) {
                            bootstrap.log(ex);
                        }
                    }
            );
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    public void createTrade(EnjPlayer inviter,
                            EnjPlayer invited,
                            List<ItemStack> inviterOffer,
                            List<ItemStack> invitedOffer) {
        if (inviter == null || invited == null)
            throw new NullPointerException("Inviter or invited EnjPlayer is null.");
        if (!inviter.isLinked() || !invited.isLinked())
            throw new IllegalArgumentException("Inviter or invited EnjPlayer is not linked.");

        Player bukkitPlayerOne = inviter.getBukkitPlayer();
        Player bukkitPlayerTwo = invited.getBukkitPlayer();

        List<TokenValueData> playerOneTokens = extractOffers(inviterOffer);
        List<TokenValueData> playerTwoTokens = extractOffers(invitedOffer);

        TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
        client.getRequestService().createRequestAsync(new CreateRequest()
                .appId(client.getAppId())
                .identityId(inviter.getIdentityId())
                .createTrade(CreateTradeData.builder()
                                            .offeringTokens(playerOneTokens)
                                            .askingTokens(playerTwoTokens)
                                            .secondPartyIdentityId(invited.getIdentityId())
                                            .build()),
                networkResponse -> {
                    try {
                        if (!networkResponse.isSuccess())
                            throw new NetworkException(networkResponse.code());

                        GraphQLResponse<Transaction> graphQLResponse = networkResponse.body();
                        if (!graphQLResponse.isSuccess())
                            throw new GraphQLException(graphQLResponse.getErrors());

                        Transaction dataIn = graphQLResponse.getData();
                        Translation.COMMAND_TRADE_CONFIRM_WAIT.send(bukkitPlayerTwo);
                        Translation.COMMAND_TRADE_CONFIRM_ACTION.send(bukkitPlayerOne);

                        bootstrap.db().createTrade(bukkitPlayerOne.getUniqueId(),
                                inviter.getIdentityId(),
                                inviter.getEthereumAddress(),
                                bukkitPlayerTwo.getUniqueId(),
                                invited.getIdentityId(),
                                invited.getEthereumAddress(),
                                dataIn.getId());
                    } catch (Exception ex) {
                        bootstrap.log(ex);
                    }
                }
        );
    }

    public void cancelTrade(Integer requestId) {
        try {
            bootstrap.db().cancelTrade(requestId);
        } catch (SQLException ex) {
            bootstrap.log(ex);
        }
    }

    private List<TokenValueData> extractOffers(List<ItemStack> offers) {
        Map<String, Integer> tokens = new HashMap<>();

        for (ItemStack is : offers) {
            String tokenId = TokenUtils.getTokenID(is);
            if (StringUtils.isEmpty(tokenId))
                continue;
            tokens.compute(tokenId, (key, value) -> value == null ? is.getAmount() : value + is.getAmount());
        }

        return tokens.entrySet().stream()
                .map(e -> TokenValueData.builder()
                        .id(e.getKey())
                        .value(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onEnjPlayerQuit(EnjPlayerQuitEvent event) {
        EnjPlayer player = event.getPlayer();

        player.getSentTradeInvites().forEach(other -> other.getReceivedTradeInvites().remove(player));
        player.getReceivedTradeInvites().forEach(other -> other.getSentTradeInvites().remove(player));

        player.getBukkitPlayer().closeInventory();
    }

}
