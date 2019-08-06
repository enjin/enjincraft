package com.enjin.ecmp.spigot.trade;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.events.EnjinCoinPlayerQuitEvent;
import com.enjin.ecmp.spigot.player.EnjinCoinPlayer;
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

    public boolean inviteExists(EnjinCoinPlayer sender, EnjinCoinPlayer target) {
        return sender.getSentTradeInvites().contains(target);
    }

    public boolean addInvite(EnjinCoinPlayer sender, EnjinCoinPlayer target) {
        boolean result = !inviteExists(sender, target);

        if (result) {
            sender.getSentTradeInvites().add(target);
            target.getReceivedTradeInvites().add(sender);
        }

        return result;
    }

    public boolean acceptInvite(EnjinCoinPlayer sender, EnjinCoinPlayer target) {
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

    public boolean declineInvite(EnjinCoinPlayer sender, EnjinCoinPlayer target) {
        sender.getSentTradeInvites().remove(target);
        return target.getReceivedTradeInvites().remove(sender);
    }

    public void completeTrade(String requestId) {
        Trade trade = tradesPendingCompletion.remove(requestId);
        if (trade != null) {
            PlayerManager playerManager = bootstrap.getPlayerManager();
            EnjinCoinPlayer playerOne = playerManager.getPlayer(trade.getPlayerOneUuid());
            EnjinCoinPlayer playerTwo = playerManager.getPlayer(trade.getPlayerTwoUuid());

            if (playerOne != null && playerTwo != null) {
                Player bukkitPlayerOne = playerOne.getBukkitPlayer();
                Player bukkitPlayerTwo = playerTwo.getBukkitPlayer();

                bukkitPlayerOne.getInventory().addItem(trade.getPlayerTwoOffer().toArray(new ItemStack[0]));
                bukkitPlayerTwo.getInventory().addItem(trade.getPlayerOneOffer().toArray(new ItemStack[0]));

                TextComponent text = TextComponent.builder()
                        .content("Your trade is complete!")
                        .color(TextColor.GRAY)
                        .build();

                if (bukkitPlayerOne != null && bukkitPlayerOne.isOnline()) {
                    MessageUtils.sendComponent(bukkitPlayerOne, text);
                }

                if (bukkitPlayerTwo != null && bukkitPlayerTwo.isOnline()) {
                    MessageUtils.sendComponent(bukkitPlayerTwo, text);
                }
            }
        }
    }

    public void submitCompleteTrade(String requestId, String tradeId) {
        Trade trade = tradesPendingCompletion.remove(requestId);

        if (trade == null) return;

        trade.setTradeId(tradeId);

        PlayerManager playerManager = bootstrap.getPlayerManager();
        EnjinCoinPlayer playerOne = playerManager.getPlayer(trade.getPlayerOneUuid());
        EnjinCoinPlayer playerTwo = playerManager.getPlayer(trade.getPlayerTwoUuid());

        if (playerOne != null && playerTwo != null) {
            if (playerOne.isIdentityLoaded() && playerTwo.isIdentityLoaded()) {
                TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
                RequestsService service = client.getRequestsService();
                Player bukkitPlayerOne = playerOne.getBukkitPlayer();
                Player bukkitPlayerTwo = playerTwo.getBukkitPlayer();

                service.createRequestAsync(
                        new CreateRequest().identityId(playerTwo.getIdentityId())
                                .completeTrade(CompleteTradeData.builder()
                                        .tradeId(trade.getTradeId())
                                        .build()),
                        response -> {
                            if (response.body() != null) {
                                if (response.body() != null) {
                                    GraphQLResponse<Transaction> body = response.body();

                                    if (body.getData() != null) {
                                        Transaction dataIn = body.getData();

                                        if (bukkitPlayerOne != null && bukkitPlayerOne.isOnline()) {
                                            MessageUtils.sendComponent(bukkitPlayerOne, wait);
                                        }

                                        if (bukkitPlayerTwo != null && bukkitPlayerTwo.isOnline()) {
                                            MessageUtils.sendComponent(bukkitPlayerTwo, action);
                                        }

                                        String key = dataIn.getId().toString();
                                        tradesPendingCompletion.put(key, trade);
                                    }
                                }
                            } else {
                                TextComponent text = TextComponent.builder()
                                        .content("An error occurred when completing your trade.")
                                        .color(TextColor.RED)
                                        .build();

                                if (bukkitPlayerOne != null && bukkitPlayerOne.isOnline()) {
                                    MessageUtils.sendComponent(bukkitPlayerOne, text);
                                }

                                if (bukkitPlayerTwo != null && bukkitPlayerTwo.isOnline()) {
                                    MessageUtils.sendComponent(bukkitPlayerTwo, text);

                                    if (!(response.isSuccess() || response.isEmpty())) {
                                        GraphQLResponse<?> body = response.body();
                                        if (body.getErrors() != null) {
                                            for (GraphQLError error : body.getErrors()) {
                                                MessageUtils.sendComponent(bukkitPlayerOne, TextComponent.builder()
                                                        .content(error.getMessage())
                                                        .color(TextColor.RED)
                                                        .build());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                );
            }
        }
    }

    public void submitCreateTrade(Trade trade) {
        PlayerManager playerManager = bootstrap.getPlayerManager();
        EnjinCoinPlayer playerOne = playerManager.getPlayer(trade.getPlayerOneUuid());
        EnjinCoinPlayer playerTwo = playerManager.getPlayer(trade.getPlayerTwoUuid());

        if (playerOne != null && playerTwo != null) {
            if (playerOne.isIdentityLoaded() && playerTwo.isIdentityLoaded()) {
                TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
                RequestsService service = client.getRequestsService();
                Player bukkitPlayerOne = playerOne.getBukkitPlayer();
                Player bukkitPlayerTwo = playerTwo.getBukkitPlayer();

                List<TokenValueData> playerOneTokens = extractTokens(trade.getPlayerOneOffer());
                List<TokenValueData> playerTwoTokens = extractTokens(trade.getPlayerTwoOffer());

                service.createRequestAsync(
                        new CreateRequest().identityId(playerOne.getIdentityId())
                                .createTrade(CreateTradeData.builder()
                                        .offeringTokens(playerOneTokens)
                                        .askingTokens(playerTwoTokens)
                                        .secondPartyIdentityId(playerTwo.getIdentityId())
                                        .build()),
                        response -> {
                            if (response.body() != null) {
                                if (response.body() != null) {
                                    GraphQLResponse<Transaction> body = response.body();

                                    if (body.getData() != null) {
                                        Transaction dataIn = body.getData();

                                        if (bukkitPlayerOne != null && bukkitPlayerOne.isOnline()) {
                                            MessageUtils.sendComponent(bukkitPlayerOne, action);
                                        }

                                        if (bukkitPlayerTwo != null && bukkitPlayerTwo.isOnline()) {
                                            MessageUtils.sendComponent(bukkitPlayerTwo, wait);
                                        }

                                        String key = dataIn.getId().toString();
                                        tradesPendingCompletion.put(key, trade);
                                    }
                                }
                            } else {
                                TextComponent text = TextComponent.builder()
                                        .content("An error occurred when creating your trade.")
                                        .color(TextColor.RED)
                                        .build();

                                if (bukkitPlayerOne != null && bukkitPlayerOne.isOnline()) {
                                    MessageUtils.sendComponent(bukkitPlayerOne, text);

                                    if (!(response.isSuccess() || response.isEmpty())) {
                                        GraphQLResponse<?> body = response.body();
                                        if (body.getErrors() != null) {
                                            for (GraphQLError error : body.getErrors()) {
                                                MessageUtils.sendComponent(bukkitPlayerOne, TextComponent.builder()
                                                        .content(error.getMessage())
                                                        .color(TextColor.RED)
                                                        .build());
                                            }
                                        }
                                    }
                                }

                                if (bukkitPlayerTwo != null && bukkitPlayerTwo.isOnline()) {
                                    MessageUtils.sendComponent(bukkitPlayerTwo, text);
                                }
                            }
                        }
                );
            }
        }
    }

    private List<TokenValueData> extractTokens(List<ItemStack> offeredItems) {
        List<TokenValueData> offers = new ArrayList<>();

        for (ItemStack item : offeredItems) {
            NBTItem nbtItem = new NBTItem(item);
            if (nbtItem.hasKey("tokenID")) {
                String tokenId = nbtItem.getString("tokenID");

                offers.add(TokenValueData.builder()
                        .id(tokenId)
                        .value(item.getAmount())
                        .build());
            }
        }

        return offers;
    }

    @EventHandler
    public void onEnjinCoinPlayerQuit(EnjinCoinPlayerQuitEvent event) {
        EnjinCoinPlayer player = event.getPlayer();

        player.getSentTradeInvites().forEach(other -> other.getReceivedTradeInvites().remove(player));
        player.getReceivedTradeInvites().forEach(other -> other.getSentTradeInvites().remove(player));

        TradeView tradeView = player.getActiveTradeView();
        if (tradeView != null && tradeView.getOther() != null) {
            player.getBukkitPlayer().closeInventory();
        }
    }

}
