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
import com.enjin.sdk.models.request.data.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            boolean removedFromInvitee = invitee.getReceivedTradeInvites().remove(inviter);

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

        Optional<Player> inviter = Optional.ofNullable(Bukkit.getPlayer(session.getInviterUuid()));
        Optional<Player> invitee = Optional.ofNullable(Bukkit.getPlayer(session.getInvitedUuid()));

        inviter.ifPresent(Translation.COMMAND_TRADE_COMPLETE::send);
        invitee.ifPresent(Translation.COMMAND_TRADE_COMPLETE::send);

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

        Optional<Player> inviter = Optional.ofNullable(Bukkit.getPlayer(session.getInviterUuid()));
        Optional<Player> invitee = Optional.ofNullable(Bukkit.getPlayer(session.getInvitedUuid()));

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
                    invitee.ifPresent(Translation.COMMAND_TRADE_CONFIRM_ACTION::send);

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

        if (inviterOffer.isEmpty() && invitedOffer.isEmpty())
            return;
        else if (inviterOffer.isEmpty())
            send(invitee, inviter, invitedOffer);
        else if (invitedOffer.isEmpty())
            send(inviter, invitee, inviterOffer);
        else
            createTradeRequest(inviter, invitee, extractOffers(inviterOffer), extractOffers(invitedOffer));
    }

    private void send(EnjPlayer inviter, EnjPlayer invitee, List<ItemStack> tokens) {
        TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
        CreateRequest input = new CreateRequest()
                .appId(client.getAppId())
                .identityId(inviter.getIdentityId());

        if (tokens.size() == 1) {
            ItemStack is = tokens.get(0);
            SendTokenData.SendTokenDataBuilder builder = SendTokenData.builder();
            builder.recipientIdentityId(invitee.getIdentityId())
                    .tokenId(TokenUtils.getTokenID(is))
                    .value(is.getAmount());

            if (TokenUtils.isNonFungible(is))
                builder.tokenIndex(TokenUtils.getTokenIndex(is));

            input.sendToken(builder.build());
        } else {
            List<TransferData> transfers = new ArrayList<>();

            for (ItemStack is : tokens) {
                TransferData.TransferDataBuilder builder = TransferData.builder()
                        .fromId(inviter.getIdentityId())
                        .toId(invitee.getIdentityId())
                        .tokenId(TokenUtils.getTokenID(is))
                        .value(String.valueOf(is.getAmount()));

                if (TokenUtils.isNonFungible(is))
                    builder.tokenIndex(TokenUtils.getTokenIndex(is));

                transfers.add(builder.build());
            }

            input.advancedSendToken(AdvancedSendTokenData.builder()
                    .transfers(transfers)
                    .build());
        }

        client.getRequestService().createRequestAsync(input, networkResponse -> {
            if (!networkResponse.isSuccess())
                throw new NetworkException(networkResponse.code());

            GraphQLResponse<Transaction> graphQLResponse = networkResponse.body();
            if (!graphQLResponse.isSuccess())
                throw new GraphQLException(graphQLResponse.getErrors());

            Translation.COMMAND_TRADE_CONFIRM_WAIT.send(invitee.getBukkitPlayer());
            Translation.COMMAND_TRADE_CONFIRM_ACTION.send(inviter.getBukkitPlayer());
        });
    }

    private void createTradeRequest(EnjPlayer inviter, EnjPlayer invitee, List<TokenValueData> playerOneTokens, List<TokenValueData> playerTwoTokens) {
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
                        Translation.COMMAND_TRADE_CONFIRM_WAIT.send(invitee.getBukkitPlayer());
                        Translation.COMMAND_TRADE_CONFIRM_ACTION.send(inviter.getBukkitPlayer());

                        bootstrap.db().createTrade(inviter.getBukkitPlayer().getUniqueId(),
                                inviter.getIdentityId(),
                                inviter.getEthereumAddress(),
                                invitee.getBukkitPlayer().getUniqueId(),
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
                String index = TokenUtils.getTokenIndex(is);
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
