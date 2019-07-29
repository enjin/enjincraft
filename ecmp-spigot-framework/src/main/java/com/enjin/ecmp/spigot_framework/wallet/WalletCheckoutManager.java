package com.enjin.ecmp.spigot_framework.wallet;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.ecmp.spigot_framework.util.TokenUtils;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WalletCheckoutManager {

    private static Map<String, ItemStack> checkedOutTokens;

    private UUID playerUuid;

    public WalletCheckoutManager(UUID playerUuid) {
        checkedOutTokens = new HashMap<>();
        this.playerUuid = playerUuid;
    }

    public Map<String, ItemStack> accessCheckout() {
        return checkedOutTokens;
    }

    public boolean populate(BasePlugin main, Player player, LegacyWallet wallet) {
        PlayerInventory playerInventory = player.getInventory();

        // Fetch configured tokens from the config.
        JsonObject tokensDisplayConfig = main.getBootstrap().getConfig().get("tokens").getAsJsonObject();

        Map<String, ItemStack> allItems = new ConcurrentHashMap<>();

        // gather all held items into one list for processing.
        List<ItemStack> allHeldItems = new ArrayList<>();
        if (playerInventory.getContents() != null)
            allHeldItems.addAll(Arrays.asList(playerInventory.getContents()));

        // handle inventory contents
        for (int i = 0; i < allHeldItems.size(); i++) {
            String tokenId = TokenUtils.getTokenID(allHeldItems.get(i));

            if (tokenId == null || tokenId.isEmpty())
                continue; // skip this item as it did not contain a potential token id

            ItemStack clone = allHeldItems.get(i).clone();

            if (allItems.get(tokenId) != null) { // item found with this tokenId
                // tally up the items found
                int total = allItems.get(tokenId).getAmount() + clone.getAmount();
                allItems.get(tokenId).setAmount(total);
            } else {
                // check to see if this is a tracked item from the plugin config
                JsonObject tokenDisplay = tokensDisplayConfig.has(String.valueOf(tokenId))
                        ? tokensDisplayConfig.get(String.valueOf(tokenId)).getAsJsonObject()
                        : null;

                if (tokenDisplay != null) {
                    // add the newly found item
                    allItems.put(tokenId, clone);
                }
            }
        }

        // clear and set the checked out item map.
        checkedOutTokens.clear();
        checkedOutTokens = new HashMap<>(allItems);

        Inventory walletInventory = wallet.getInventory();
        // update the wallet inventory to reflect the currently checked out totals.
        List<ItemStack> allWalletItems = new ArrayList<>();
        if (walletInventory != null)
            allWalletItems.addAll(Arrays.asList(walletInventory.getContents()));

        Map<String, ItemStack> walletItems = new HashMap<>();

        for (ItemStack itemStack : allWalletItems) {
            String tokenId = TokenUtils.getTokenID(itemStack);

            if (tokenId == null || tokenId.isEmpty()) continue; // this shouldn't happen but just in case...

            ItemStack clone = itemStack.clone();

            // logic assumes all items are in root inventory and not within storage -- as such, we're going to collate the
            // item stacks into a map similar to the AllItems list.
            if (walletItems.get(tokenId) != null) { // found item
                int total = walletItems.get(tokenId).getAmount() + clone.getAmount();
                walletItems.get(tokenId).setAmount(total);
            } else { // item not found, lets add it.
                // lets make sure the token is part of the valid server token set.
                JsonObject tokenDisplay = tokensDisplayConfig.has(String.valueOf(tokenId))
                        ? tokensDisplayConfig.get(String.valueOf(tokenId)).getAsJsonObject()
                        : null;

                if (tokenDisplay != null) {
                    walletItems.put(tokenId, clone);
                }
            }
        }

        for (String tokenId : wallet.getTokenBalances().keySet()) {
            if (wallet.getTokenBalances() != null && checkedOutTokens.get(tokenId) != null)
                break;
//                wallet.getTokenBalances().get(tokenId).setCheckedOut(checkedOutTokens.get(tokenId).getAmount());
        }

        return true;
    }

    public boolean checkoutItem(ItemStack itemStack) {
        String tokenId = TokenUtils.getTokenID(itemStack);

        if (checkedOutTokens.get(tokenId) == null) {
            // handle first checkout of an item of this ID.
            checkedOutTokens.put(tokenId, itemStack);
            return true;
        } else {
            // check out additional items of this ID.
            ItemStack stack = checkedOutTokens.get(tokenId);
            int balance = itemStack.getAmount() + stack.getAmount();
            if (balance >= 0) {
                // update the checked out balance for the given item.
                itemStack.setAmount(balance);
                checkedOutTokens.replace(tokenId, itemStack);
                return true;
            } else {
                // balance was a negative number?!
                return false;
            }
        }
    }

    public boolean returnItem(ItemStack itemStack) {
        String tokenId = TokenUtils.getTokenID(itemStack);

        if (checkedOutTokens.get(tokenId) == null) {
            // no items of this type were registered as checked out or the item is not a valid wallet item.
            return false;
        } else {
            ItemStack stack = checkedOutTokens.get(tokenId);
            int balance = stack.getAmount() - itemStack.getAmount();
            if (balance > 0) {
                // update the checked out balance for the given item.
                itemStack.setAmount(balance);
                checkedOutTokens.replace(tokenId, itemStack);
                return true;
            } else { // handle values of zero or less (negative numbers?!)
                // remove the tokenId from tracking if we've returned all the checked out copies.
                checkedOutTokens.remove(tokenId);
                return true;
            }
        }
    }
}
