package com.enjin.enjincoin.spigot_framework.trade;

import com.enjin.enjincoin.sdk.client.Client;
import com.enjin.enjincoin.sdk.client.model.body.GraphQLResponse;
import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.sdk.client.service.requests.RequestsService;
import com.enjin.enjincoin.sdk.client.service.requests.vo.TransactionType;
import com.enjin.enjincoin.sdk.client.service.requests.vo.data.CreateRequestData;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager implements Listener {

    private BasePlugin plugin;
    private Map<Integer, Trade> tradesAwaitingWalletApproval = new ConcurrentHashMap<>();
    private Map<String, Trade> tradesAwaitingBlockChainConfirmation = new ConcurrentHashMap<>();

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
        }

        return result;
    }

    public void submit(Trade trade) {
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

                JsonObject data = new JsonObject();
                JsonArray playerOneTokens = extractTokens(trade.getPlayerOneOffer());
                JsonArray playerTwoTokens = extractTokens(trade.getPlayerTwoOffer());

                data.add("offering_tokens", playerOneTokens);
                data.add("asking_tokens", playerTwoTokens);
                data.addProperty("second_party_identity_id", playerTwoIdentity.getId());

                service.createRequestAsync(playerOneIdentity.getId(), null, TransactionType.CREATE_TRADE, null,
                        null, null, null, data, null, null,
                        null, new Callback<GraphQLResponse<CreateRequestData>>() {
                            @Override
                            public void onResponse(Call<GraphQLResponse<CreateRequestData>> call, Response<GraphQLResponse<CreateRequestData>> response) {
                                if (response.isSuccessful()) {
                                    if (response.body() != null) {
                                        GraphQLResponse<CreateRequestData> body = response.body();

                                        if (body.getData() != null) {
                                            CreateRequestData data = body.getData();

                                            TextComponent text = TextComponent.builder()
                                                    .content("Please confirm the trade in your Enjin wallet!")
                                                    .color(TextColor.GRAY)
                                                    .build();

                                            if (bukkitPlayerOne != null && bukkitPlayerOne.isOnline()) {
                                                MessageUtils.sendMessage(bukkitPlayerOne, text);
                                            }

                                            if (bukkitPlayerTwo != null && bukkitPlayerTwo.isOnline()) {
                                                MessageUtils.sendMessage(bukkitPlayerTwo, text);
                                            }

                                            tradesAwaitingWalletApproval.put(data.getRequest().getId(), trade);
                                        }
                                    }
                                } else {
                                    TextComponent text = TextComponent.builder()
                                            .content("An error occurred when creating your trade.")
                                            .color(TextColor.RED)
                                            .build();

                                    if (bukkitPlayerOne != null && bukkitPlayerOne.isOnline()) {
                                        MessageUtils.sendMessage(bukkitPlayerOne, text);
                                    }

                                    if (bukkitPlayerTwo != null && bukkitPlayerTwo.isOnline()) {
                                        MessageUtils.sendMessage(bukkitPlayerTwo, text);
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<GraphQLResponse<CreateRequestData>> call, Throwable t) {
                                plugin.getLogger().warning(t.toString());
                            }
                        });
            }
        }
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
