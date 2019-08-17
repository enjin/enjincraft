package com.enjin.ecmp.spigot.trade;

import com.enjin.ecmp.spigot.GraphQLException;
import com.enjin.ecmp.spigot.NetworkException;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.events.EnjPlayerQuitEvent;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.player.PlayerManager;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.graphql.GraphQLError;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.model.service.requests.CreateRequest;
import com.enjin.enjincoin.sdk.model.service.requests.Transaction;
import com.enjin.enjincoin.sdk.model.service.requests.data.CompleteTradeData;
import com.enjin.enjincoin.sdk.model.service.requests.data.CreateTradeData;
import com.enjin.enjincoin.sdk.model.service.requests.data.TokenValueData;
import com.enjin.enjincoin.sdk.service.requests.RequestsService;
import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager implements Listener {

    private SpigotBootstrap bootstrap;
    private Map<String, Trade> tradesPendingCompletion = new ConcurrentHashMap<>();

    TextComponent action = TextComponent.builder()
            .content("Please confirm the trade in your Enjin wallet!")
            .color(TextColor.GRAY)
            .build();
    TextComponent wait = TextComponent.builder()
            .content("Please wait while the other player confirms the trade.")
            .color(TextColor.GRAY)
            .build();

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

    public boolean acceptInvite(EnjPlayer sender, EnjPlayer target) {
        boolean result = inviteExists(sender, target);

        if (result) {
            sender.getSentTradeInvites().remove(target);
            target.getReceivedTradeInvites().remove(target);

            sender.setActiveTradeView(new TradeView(bootstrap, sender, target));
            target.setActiveTradeView(new TradeView(bootstrap, target, sender));

            sender.getActiveTradeView().open();
            target.getActiveTradeView().open();
        }

        return result;
    }

    public boolean declineInvite(EnjPlayer sender, EnjPlayer target) {
        sender.getSentTradeInvites().remove(target);
        return target.getReceivedTradeInvites().remove(sender);
    }

    public void completeTrade(String requestId) {
        Trade trade = tradesPendingCompletion.remove(requestId);

        if (trade == null) return;

        EnjPlayer playerOne = bootstrap.getPlayerManager().getPlayer(trade.getPlayerOneUuid()).orElse(null);
        EnjPlayer playerTwo = bootstrap.getPlayerManager().getPlayer(trade.getPlayerTwoUuid()).orElse(null);

        if (playerOne == null || playerTwo == null) return;

        Player bukkitPlayerOne = playerOne.getBukkitPlayer();
        Player bukkitPlayerTwo = playerTwo.getBukkitPlayer();

        bukkitPlayerOne.getInventory().addItem(trade.getPlayerTwoOffer().toArray(new ItemStack[0]));
        bukkitPlayerTwo.getInventory().addItem(trade.getPlayerOneOffer().toArray(new ItemStack[0]));

        TextComponent text = TextComponent.builder()
                .content("Your trade is complete!")
                .color(TextColor.GRAY)
                .build();

        MessageUtils.sendComponent(bukkitPlayerOne, text);
        MessageUtils.sendComponent(bukkitPlayerTwo, text);
    }

    public void submitCompleteTrade(String requestId, String tradeId) {
        Trade trade = tradesPendingCompletion.remove(requestId);

        if (trade == null) return;

        trade.setTradeId(tradeId);

        EnjPlayer playerOne = bootstrap.getPlayerManager().getPlayer(trade.getPlayerOneUuid()).orElse(null);
        EnjPlayer playerTwo = bootstrap.getPlayerManager().getPlayer(trade.getPlayerTwoUuid()).orElse(null);

        if (playerOne == null || playerTwo == null) return;
        if (!playerOne.isLinked() || !playerTwo.isLinked()) return;

        Player bukkitPlayerOne = playerOne.getBukkitPlayer();
        Player bukkitPlayerTwo = playerTwo.getBukkitPlayer();

        bootstrap.getTrustedPlatformClient()
                .getRequestsService().createRequestAsync(new CreateRequest()
                        .identityId(playerTwo.getIdentityId())
                        .completeTrade(CompleteTradeData.builder()
                                .tradeId(trade.getTradeId())
                                .build()),
                networkResponse -> {
                    if (!networkResponse.isSuccess()) throw new NetworkException(networkResponse.code());

                    GraphQLResponse<Transaction> graphQLResponse = networkResponse.body();
                    if (!graphQLResponse.isSuccess()) throw new GraphQLException(graphQLResponse.getErrors());

                    Transaction dataIn = graphQLResponse.getData();
                    MessageUtils.sendComponent(bukkitPlayerOne, wait);
                    MessageUtils.sendComponent(bukkitPlayerTwo, action);

                    String key = dataIn.getId().toString();
                    tradesPendingCompletion.put(key, trade);
                }
        );
    }

    public void submitCreateTrade(Trade trade) {
        EnjPlayer playerOne = bootstrap.getPlayerManager().getPlayer(trade.getPlayerOneUuid()).orElse(null);
        EnjPlayer playerTwo = bootstrap.getPlayerManager().getPlayer(trade.getPlayerTwoUuid()).orElse(null);

        if (playerOne == null || playerTwo == null) return;
        if (!playerOne.isLinked() || !playerTwo.isLinked()) return;

        Player bukkitPlayerOne = playerOne.getBukkitPlayer();
        Player bukkitPlayerTwo = playerTwo.getBukkitPlayer();

        List<TokenValueData> playerOneTokens = extractTokens(trade.getPlayerOneOffer());
        List<TokenValueData> playerTwoTokens = extractTokens(trade.getPlayerTwoOffer());

        bootstrap.getTrustedPlatformClient()
                .getRequestsService().createRequestAsync(new CreateRequest()
                        .identityId(playerOne.getIdentityId())
                        .createTrade(CreateTradeData.builder()
                                .offeringTokens(playerOneTokens)
                                .askingTokens(playerTwoTokens)
                                .secondPartyIdentityId(playerTwo.getIdentityId())
                                .build()),
                networkResponse -> {
                    if (!networkResponse.isSuccess()) throw new NetworkException(networkResponse.code());

                    GraphQLResponse<Transaction> graphQLResponse = networkResponse.body();
                    if (!graphQLResponse.isSuccess()) throw new GraphQLException(graphQLResponse.getErrors());

                    Transaction dataIn = graphQLResponse.getData();
                    MessageUtils.sendComponent(bukkitPlayerOne, action);
                    MessageUtils.sendComponent(bukkitPlayerTwo, wait);

                    String key = dataIn.getId().toString();
                    tradesPendingCompletion.put(key, trade);
                }
        );
    }

    private List<TokenValueData> extractTokens(List<ItemStack> offeredItems) {
        List<TokenValueData> offers = new ArrayList<>();

        for (ItemStack item : offeredItems) {
            NBTItem nbtItem = new NBTItem(item);
            if (!nbtItem.hasKey("tokenID")) continue;
            String tokenId = nbtItem.getString("tokenID");
            offers.add(TokenValueData.builder()
                    .id(tokenId)
                    .value(item.getAmount())
                    .build());
        }

        return offers;
    }

    @EventHandler
    public void onEnjinCoinPlayerQuit(EnjPlayerQuitEvent event) {
        EnjPlayer player = event.getPlayer();

        player.getSentTradeInvites().forEach(other -> other.getReceivedTradeInvites().remove(player));
        player.getReceivedTradeInvites().forEach(other -> other.getSentTradeInvites().remove(player));

        player.getBukkitPlayer().closeInventory();
    }

}
