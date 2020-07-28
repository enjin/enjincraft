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

public class TradeManager implements Listener {

    private final SpigotBootstrap bootstrap;

    public TradeManager(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public boolean inviteExists(EnjPlayer sender, EnjPlayer target) {
        return sender.getSentTradeInvites().contains(target);
    }

    public boolean addInvite(EnjPlayer inviter, EnjPlayer invitee) {
        if (!inviteExists(inviter, invitee)) {
            inviter.getSentTradeInvites().add(invitee);
            invitee.getReceivedTradeInvites().add(inviter);
            return true;
        }

        return false;
    }

    public boolean acceptInvite(EnjPlayer inviter, EnjPlayer invitee) throws UnregisterTradeInviteException {
        if (inviteExists(inviter, invitee)) {
            boolean removedFromInviter = inviter.getSentTradeInvites().remove(invitee);
            boolean removedFromInvitee = invitee.getReceivedTradeInvites().remove(invitee);
            if (removedFromInviter && removedFromInvitee) {
                inviter.setActiveTradeView(new TradeView(bootstrap, inviter, invitee, Trader.INVITER));
                invitee.setActiveTradeView(new TradeView(bootstrap, invitee, inviter, Trader.INVITED));
                inviter.getActiveTradeView().open();
                invitee.getActiveTradeView().open();
                return true;
            }

            throw new UnregisterTradeInviteException(inviter, invitee);
        }

        return false;
    }

    public boolean declineInvite(EnjPlayer inviter, EnjPlayer invitee) throws UnregisterTradeInviteException {
        if (inviteExists(inviter, invitee)) {
            boolean removedFromInviter = inviter.getSentTradeInvites().remove(invitee);
            boolean removedFromInvitee = invitee.getReceivedTradeInvites().remove(inviter);
            if (removedFromInviter && removedFromInvitee)
                return true;

            throw new UnregisterTradeInviteException(inviter, invitee);
        }

        return false;
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
        if (session == null)
            return;

        Player inviter = Bukkit.getPlayer(session.getInviterUuid());
        Player invitee = Bukkit.getPlayer(session.getInvitedUuid());

        if (inviter != null)
            Translation.COMMAND_TRADE_COMPLETE.send(inviter);
        if (invitee != null)
            Translation.COMMAND_TRADE_COMPLETE.send(invitee);

        try {
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
        if (session == null || StringUtils.isEmpty(tradeId))
            return;

        Player inviter = Bukkit.getPlayer(session.getInviterUuid());
        Player invitee = Bukkit.getPlayer(session.getInvitedUuid());

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
                    if (inviter != null)
                        Translation.COMMAND_TRADE_CONFIRM_WAIT.send(inviter);
                    if (invitee != null)
                        Translation.COMMAND_TRADE_CONFIRM_ACTION.send(invitee);

                    try {
                        bootstrap.db().completeTrade(session.getCreateRequestId(), dataIn.getId(), tradeId);
                    } catch (SQLException ex) {
                        bootstrap.log(ex);
                    }
                }
        );
    }

    public void createTrade(EnjPlayer inviter,
                            EnjPlayer invitee,
                            List<ItemStack> inviterOffer,
                            List<ItemStack> invitedOffer) throws IllegalArgumentException, NullPointerException {
        if (inviter == null || invitee == null)
            throw new NullPointerException("Inviter or invited EnjPlayer is null");
        else if (!inviter.isLinked() || !invitee.isLinked())
            throw new IllegalArgumentException("Inviter or invited EnjPlayer is not linked");

        Player bukkitPlayerOne = inviter.getBukkitPlayer();
        Player bukkitPlayerTwo = invitee.getBukkitPlayer();

        List<TokenValueData> playerOneTokens = extractOffers(inviterOffer);
        List<TokenValueData> playerTwoTokens = extractOffers(invitedOffer);

        TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
        client.getRequestService().createRequestAsync(new CreateRequest()
                .appId(client.getAppId())
                .identityId(inviter.getIdentityId())
                .createTrade(CreateTradeData.builder()
                                            .offeringTokens(playerOneTokens)
                                            .askingTokens(playerTwoTokens)
                                            .secondPartyIdentityId(invitee.getIdentityId())
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
                                invitee.getIdentityId(),
                                invitee.getEthereumAddress(),
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
        List<TokenValueData> extractedOffers = new ArrayList<>();

        for (ItemStack is : offers) {
            if (!TokenUtils.isValidTokenItem(is))
                continue;

            int value = is.getAmount();
            String id = TokenUtils.getTokenID(is);

            if (TokenUtils.isNonFungible(is)) {
                String  index    = TokenUtils.getTokenIndex(is);
                Integer intIndex = TokenUtils.convertIndexToLong(index).intValue();

                extractedOffers.add(TokenValueData.builder()
                        .id(id)
                        .index(intIndex)
                        .value(value)
                        .build());
            } else {
                extractedOffers.add(TokenValueData.builder()
                        .id(id)
                        .value(value)
                        .build());
            }
        }

        return extractedOffers;
    }

    @EventHandler
    public void onEnjPlayerQuit(EnjPlayerQuitEvent event) {
        EnjPlayer player = event.getPlayer();

        player.getSentTradeInvites().forEach(other -> other.getReceivedTradeInvites().remove(player));
        player.getReceivedTradeInvites().forEach(other -> other.getSentTradeInvites().remove(player));

        player.getBukkitPlayer().closeInventory();
    }

}
