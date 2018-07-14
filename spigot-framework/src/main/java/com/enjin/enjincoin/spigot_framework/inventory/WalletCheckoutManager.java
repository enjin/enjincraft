package com.enjin.enjincoin.spigot_framework.inventory;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * class responsible for managing the persistent state of objects checkedout by a player.
 */
public class WalletCheckoutManager {

    /**
     * Inventory tracker map
     *
     * Map<TokenID, ItemStack>
     */
    private Map<String, ItemStack> checkedOutTokens;

    private UUID playerId;

    public WalletCheckoutManager(UUID playerId) {
        checkedOutTokens = new HashMap<>();
        this.playerId = playerId;
    }

    public Map<String, ItemStack> accessCheckout() {
        return checkedOutTokens;
    }
}
