package com.enjin.ecmp.spigot.trade;

import com.enjin.ecmp.spigot.GraphQLException;
import com.enjin.ecmp.spigot.NetworkException;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.Trader;
import com.enjin.ecmp.spigot.events.EnjPlayerQuitEvent;
import com.enjin.ecmp.spigot.i18n.Translation;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.player.PlayerManager;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.ecmp.spigot.util.TokenUtils;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.graphql.GraphQLError;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.model.service.requests.CreateRequest;
import com.enjin.enjincoin.sdk.model.service.requests.Transaction;
import com.enjin.enjincoin.sdk.model.service.requests.data.CompleteTradeData;
import com.enjin.enjincoin.sdk.model.service.requests.data.CreateTradeData;
import com.enjin.enjincoin.sdk.model.service.requests.data.TokenValueData;
import com.enjin.enjincoin.sdk.service.requests.RequestsService;
import com.enjin.java_commons.StringUtils;
import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

            if (session == null)
                return;

            Optional<Player> inviter = Optional.ofNullable(Bukkit.getPlayer(session.getInviterUuid()));
            Optional<Player> invited = Optional.ofNullable(Bukkit.getPlayer(session.getInvitedUuid()));

            inviter.ifPresent(player -> Translation.COMMAND_TRADE_COMPLETE.send(player));
            invited.ifPresent(player -> Translation.COMMAND_TRADE_COMPLETE.send(player));

            bootstrap.db().tradeExecuted(requestId);
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    public void completeTrade(Integer requestId, String tradeId) {
        try {
            TradeSession session = bootstrap.db().getSessionFromRequestId(requestId);

            if (session == null)
                return;

            Optional<Player> inviter = Optional.ofNullable(Bukkit.getPlayer(session.getInviterUuid()));
            Optional<Player> invited = Optional.ofNullable(Bukkit.getPlayer(session.getInvitedUuid()));

            bootstrap.getTrustedPlatformClient()
                    .getRequestsService().createRequestAsync(new CreateRequest()
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
                        inviter.ifPresent(player -> Translation.COMMAND_TRADE_CONFIRM_WAIT.send(player));
                        invited.ifPresent(player -> Translation.COMMAND_TRADE_CONFIRM_ACTION.send(player));

                        try {
                            bootstrap.db().completeTrade(requestId,
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
            return;
        if (!inviter.isLinked() || !invited.isLinked())
            return;

        Player bukkitPlayerOne = inviter.getBukkitPlayer();
        Player bukkitPlayerTwo = invited.getBukkitPlayer();

        List<TokenValueData> playerOneTokens = extractOffers(inviterOffer);
        List<TokenValueData> playerTwoTokens = extractOffers(invitedOffer);

        bootstrap.getTrustedPlatformClient()
                .getRequestsService().createRequestAsync(new CreateRequest()
                        .identityId(inviter.getIdentityId())
                        .createTrade(CreateTradeData.builder()
                                .offeringTokens(playerOneTokens)
                                .askingTokens(playerTwoTokens)
                                .secondPartyIdentityId(invited.getIdentityId())
                                .build()),
                networkResponse -> {
                    if (!networkResponse.isSuccess())
                        throw new NetworkException(networkResponse.code());

                    GraphQLResponse<Transaction> graphQLResponse = networkResponse.body();
                    if (!graphQLResponse.isSuccess())
                        throw new GraphQLException(graphQLResponse.getErrors());

                    Transaction dataIn = graphQLResponse.getData();
                    Translation.COMMAND_TRADE_CONFIRM_WAIT.send(bukkitPlayerTwo);
                    Translation.COMMAND_TRADE_CONFIRM_ACTION.send(bukkitPlayerOne);

                    try {
                        bootstrap.db().createTrade(bukkitPlayerOne.getUniqueId(),
                                inviter.getIdentityId(),
                                inviter.getEthereumAddress(),
                                bukkitPlayerTwo.getUniqueId(),
                                invited.getIdentityId(),
                                invited.getEthereumAddress(),
                                dataIn.getId());
                    } catch (SQLException ex) {
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
            tokens.compute(tokenId, (key, value) -> {
                return value == null ? is.getAmount() : value + is.getAmount();
            });
        }

        return tokens.entrySet().stream()
                .map(e -> TokenValueData.builder()
                        .id(e.getKey())
                        .value(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onEnjinCoinPlayerQuit(EnjPlayerQuitEvent event) {
        EnjPlayer player = event.getPlayer();

        player.getSentTradeInvites().forEach(other -> other.getReceivedTradeInvites().remove(player));
        player.getReceivedTradeInvites().forEach(other -> other.getSentTradeInvites().remove(player));

        player.getBukkitPlayer().closeInventory();
    }

}
