package com.enjin.enjincoin.spigot_framework.trade;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class Trade {

    private UUID playerOneUuid;
    private List<ItemStack> playerOneOffer;

    private UUID playerTwoUuid;
    private List<ItemStack> playerTwoOffer;

    public Trade(UUID playerOneUuid, List<ItemStack> playerOneOffer, UUID playerTwoUuid, List<ItemStack> playerTwoOffer) {
        this.playerOneUuid = playerOneUuid;
        this.playerOneOffer = playerOneOffer;
        this.playerTwoUuid = playerTwoUuid;
        this.playerTwoOffer = playerTwoOffer;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "playerOneUuid=" + playerOneUuid +
                ", playerOneOffer=" + playerOneOffer +
                ", playerTwoUuid=" + playerTwoUuid +
                ", playerTwoOffer=" + playerTwoOffer +
                '}';
    }
}
