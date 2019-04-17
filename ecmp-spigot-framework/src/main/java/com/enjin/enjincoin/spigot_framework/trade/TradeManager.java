package com.enjin.enjincoin.spigot_framework.trade;

import com.enjin.enjincoin.sdk.Client;
import com.enjin.enjincoin.sdk.graphql.GraphError;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.model.service.requests.CreateRequest;
import com.enjin.enjincoin.sdk.model.service.requests.CreateRequestResult;
import com.enjin.enjincoin.sdk.model.service.requests.TransactionType;
import com.enjin.enjincoin.sdk.service.requests.RequestsService;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.controllers.SdkClientController;
import com.enjin.enjincoin.spigot_framework.event.MinecraftPlayerQuitEvent;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.player.PlayerManager;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager implements Listener {

    private BasePlugin plugin;
    private Map<Integer, Trade> tradesPendingCompletion = new ConcurrentHashMap<>();

    TextComponent action = TextComponent.builder()
            .content("Please confirm the trade in your Enjin wallet!")
            .color(TextColor.GRAY)
            .build();
    TextComponent wait = TextComponent.builder()
            .content("Please wait while the other player confirms the trade.")
            .color(TextColor.GRAY)
            .build();

    public TradeManager(BasePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean inviteExists(MinecraftPlayer sender, MinecraftPlayer target) {
        return sender.getSentTradeInvites().contains(target);
    }

    public boolean addInvite(MinecraftPlayer sender, MinecraftPlayer target) {
        boolean result = !inviteExists(sender, target);

        if (result) {
            sender.getSentTradeInvites().add(target);
            target.getReceivedTradeInvites().add(sender);
        }

        return result;
    }

    public boolean acceptInvite(MinecraftPlayer sender, MinecraftPlayer target) {
        boolean result = inviteExists(sender, target);

        if (result) {
            sender.getSentTradeInvites().remove(target);
            target.getReceivedTradeInvites().remove(target);

            sender.setActiveTradeView(new TradeView(this.plugin, sender, target));
            target.setActiveTradeView(new TradeView(this.plugin, target, sender));

            sender.getActiveTradeView().open();
            target.getActiveTradeView().open();
        }

        return result;
    }

    public boolean declineInvite(MinecraftPlayer sender, MinecraftPlayer target) {
        sender.getSentTradeInvites().remove(target);
        return target.getReceivedTradeInvites().remove(sender);
    }

    public void completeTrade(int requestId) {
        Trade trade = tradesPendingCompletion.remove(requestId);
        if (trade != null) {
            PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
            MinecraftPlayer playerOne = playerManager.getPlayer(trade.getPlayerOneUuid());
            MinecraftPlayer playerTwo = playerManager.getPlayer(trade.getPlayerTwoUuid());

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
                    MessageUtils.sendMessage(bukkitPlayerOne, text);
                }

                if (bukkitPlayerTwo != null && bukkitPlayerTwo.isOnline()) {
                    MessageUtils.sendMessage(bukkitPlayerTwo, text);
                }
            }
        }
    }

    public void submitCompleteTrade(int requestId, String tradeId) {
        Trade trade = tradesPendingCompletion.remove(requestId);

        if (trade == null) return;

        trade.setTradeId(tradeId);

        PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
        MinecraftPlayer playerOne = playerManager.getPlayer(trade.getPlayerOneUuid());
        MinecraftPlayer playerTwo = playerManager.getPlayer(trade.getPlayerTwoUuid());

        if (playerOne != null && playerTwo != null) {
            Identity playerOneIdentity = playerOne.getIdentity();
            Identity playerTwoIdentity = playerTwo.getIdentity();

            if (playerOneIdentity != null && playerTwoIdentity != null) {
                SdkClientController clientController = this.plugin.getBootstrap().getSdkController();
                Client client = clientController.getClient();
                RequestsService service = client.getRequestsService();
                Player bukkitPlayerOne = playerOne.getBukkitPlayer();
                Player bukkitPlayerTwo = playerTwo.getBukkitPlayer();

                JsonObject dataOut = new JsonObject();
                dataOut.addProperty("trade_id", trade.getTradeId());

                service.createRequestAsync(
                        new CreateRequest().withIdentityId(playerTwoIdentity.getId())
                                .withType(TransactionType.COMPLETE_TRADE)
                                .withCompleteTradeData(dataOut),
                        response -> {
                            if (response.body() != null) {
                                if (response.body() != null) {
                                    GraphQLResponse<CreateRequestResult> body = response.body();

                                    if (body.getData() != null) {
                                        CreateRequestResult dataIn = body.getData();

                                        if (bukkitPlayerOne != null && bukkitPlayerOne.isOnline()) {
                                            MessageUtils.sendMessage(bukkitPlayerOne, wait);
                                        }

                                        if (bukkitPlayerTwo != null && bukkitPlayerTwo.isOnline()) {
                                            MessageUtils.sendMessage(bukkitPlayerTwo, action);
                                        }

                                        tradesPendingCompletion.put(dataIn.getRequest().getId(), trade);
                                    }
                                }
                            } else {
                                TextComponent text = TextComponent.builder()
                                        .content("An error occurred when completing your trade.")
                                        .color(TextColor.RED)
                                        .build();

                                if (bukkitPlayerOne != null && bukkitPlayerOne.isOnline()) {
                                    MessageUtils.sendMessage(bukkitPlayerOne, text);
                                }

                                if (bukkitPlayerTwo != null && bukkitPlayerTwo.isOnline()) {
                                    MessageUtils.sendMessage(bukkitPlayerTwo, text);

                                    if (response.errors() != null) {
                                        for (GraphError error : response.errors()) {
                                            MessageUtils.sendMessage(bukkitPlayerOne, TextComponent.builder()
                                                    .content(error.getMessage())
                                                    .color(TextColor.RED)
                                                    .build());
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
        PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
        MinecraftPlayer playerOne = playerManager.getPlayer(trade.getPlayerOneUuid());
        MinecraftPlayer playerTwo = playerManager.getPlayer(trade.getPlayerTwoUuid());

        if (playerOne != null && playerTwo != null) {
            Identity playerOneIdentity = playerOne.getIdentity();
            Identity playerTwoIdentity = playerTwo.getIdentity();

            if (playerOneIdentity != null && playerTwoIdentity != null) {
                SdkClientController clientController = this.plugin.getBootstrap().getSdkController();
                Client client = clientController.getClient();
                RequestsService service = client.getRequestsService();
                Player bukkitPlayerOne = playerOne.getBukkitPlayer();
                Player bukkitPlayerTwo = playerTwo.getBukkitPlayer();

                JsonObject dataOut = new JsonObject();
                JsonArray playerOneTokens = extractTokens(trade.getPlayerOneOffer());
                JsonArray playerTwoTokens = extractTokens(trade.getPlayerTwoOffer());

                dataOut.add("offering_tokens", playerOneTokens);
                dataOut.add("asking_tokens", playerTwoTokens);
                dataOut.addProperty("second_party_identity_id", playerTwoIdentity.getId());

                service.createRequestAsync(
                        new CreateRequest().withIdentityId(playerOneIdentity.getId())
                                .withType(TransactionType.CREATE_TRADE)
                                .withCreateTradeData(dataOut),
                        response -> {
                            if (response.body() != null) {
                                if (response.body() != null) {
                                    GraphQLResponse<CreateRequestResult> body = response.body();

                                    if (body.getData() != null) {
                                        CreateRequestResult dataIn = body.getData();

                                        if (bukkitPlayerOne != null && bukkitPlayerOne.isOnline()) {
                                            MessageUtils.sendMessage(bukkitPlayerOne, action);
                                        }

                                        if (bukkitPlayerTwo != null && bukkitPlayerTwo.isOnline()) {
                                            MessageUtils.sendMessage(bukkitPlayerTwo, wait);
                                        }

                                        tradesPendingCompletion.put(dataIn.getRequest().getId(), trade);
                                    }
                                }
                            } else {
                                TextComponent text = TextComponent.builder()
                                        .content("An error occurred when creating your trade.")
                                        .color(TextColor.RED)
                                        .build();

                                if (bukkitPlayerOne != null && bukkitPlayerOne.isOnline()) {
                                    MessageUtils.sendMessage(bukkitPlayerOne, text);

                                    if (response.errors() != null) {
                                        for (GraphError error : response.errors()) {
                                            MessageUtils.sendMessage(bukkitPlayerOne, TextComponent.builder()
                                                    .content(error.getMessage())
                                                    .color(TextColor.RED)
                                                    .build());
                                        }
                                    }
                                }

                                if (bukkitPlayerTwo != null && bukkitPlayerTwo.isOnline()) {
                                    MessageUtils.sendMessage(bukkitPlayerTwo, text);
                                }
                            }
                        }
                );
            }
        }
    }

    public Trade getTrade(int requestId) {
        return tradesPendingCompletion.get(requestId);
    }

    private JsonArray extractTokens(List<ItemStack> offeredItems) {
        JsonArray offer = new JsonArray();

        for (ItemStack item : offeredItems) {
            NBTItem nbtItem = new NBTItem(item);
            if (nbtItem.hasKey("tokenID")) {
                String tokenId = nbtItem.getString("tokenID");
                JsonObject tokenData = new JsonObject();

                tokenData.addProperty("id", tokenId);
                tokenData.addProperty("value", item.getAmount());

                offer.add(tokenData);
            }
        }

        return offer;
    }

    @EventHandler
    public void onMinecraftPlayerQuit(MinecraftPlayerQuitEvent event) {
        MinecraftPlayer player = event.getPlayer();

        player.getSentTradeInvites().forEach(other -> other.getReceivedTradeInvites().remove(player));
        player.getReceivedTradeInvites().forEach(other -> other.getSentTradeInvites().remove(player));

        TradeView tradeView = player.getActiveTradeView();
        if (tradeView != null && tradeView.getOther() != null) {
            player.getBukkitPlayer().closeInventory();
        }
    }

}
