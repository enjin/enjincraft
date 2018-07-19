package com.enjin.enjincoin.spigot_framework.inventory;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.player.Wallet;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import com.google.gson.JsonObject;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * class responsible for managing the persistent state of objects checkedout by a player.
 */
public class WalletCheckoutManager {

    /**
     * Inventory tracker map
     *
     * Map<TokenID, ItemStack>
     */
    private static Map<String, ItemStack> checkedOutTokens;

    private UUID playerId;

    public WalletCheckoutManager(UUID playerId) {
        checkedOutTokens = new HashMap<>();
        this.playerId = playerId;
    }

    public Map<String, ItemStack> accessCheckout() {
        return checkedOutTokens;
    }

    public String getTokenId(ItemStack itemStack) {
        if (itemStack == null) return null;

        NBTItem nbti = new NBTItem(itemStack);

        if (nbti.getString("tokenID") != null)
            return nbti.getString("tokenID");

        // lore based fallback.
        if (itemStack.getItemMeta() != null) {
            List<String> lore = itemStack.getItemMeta().getLore();
            if (lore != null) {
                if (lore.size() > 0) {
//                    System.out.println("getTokenId: " + ChatColor.stripColor(lore.get(lore.size() - 1)));
                    return ChatColor.stripColor(lore.get(lore.size() - 1));
                } else
                    return null;
            }
        }
        return null;
    }

    public boolean populate(BasePlugin main, Player player, Wallet wallet) {
        PlayerInventory playerInventory = player.getInventory();

        // Fetch configured tokens from the config.
        JsonObject tokensDisplayConfig = main.getBootstrap().getConfig().get("tokens").getAsJsonObject();

        Map<String, ItemStack> allItems = new ConcurrentHashMap<>();

        // gather all held items into one list for processing.
        List<ItemStack> allHeldItems = new ArrayList<>();
        if (playerInventory.getContents() != null)
            allHeldItems.addAll(Arrays.asList(playerInventory.getContents()));

        /**
         * these are individual calls which the getContents call encapsulates
         *
        if (playerInventory.getArmorContents() != null)
            allHeldItems.addAll(Arrays.asList(playerInventory.getArmorContents()));
        if (playerInventory.getExtraContents() != null)
            allHeldItems.addAll(Arrays.asList(playerInventory.getExtraContents()));
        if (playerInventory.getStorageContents() != null)
            allHeldItems.addAll(Arrays.asList(playerInventory.getStorageContents()));
        if (playerInventory.getItemInMainHand() != null)
            allHeldItems.add(playerInventory.getItemInMainHand());
        if (playerInventory.getItemInOffHand() != null)
            allHeldItems.add(playerInventory.getItemInOffHand());
         */

        // handle inventory contents
        for (int i = 0; i < allHeldItems.size(); i++) {
            String tokenId = getTokenId(allHeldItems.get(i));

            if (tokenId == null || tokenId.isEmpty()) continue; // skip this item as it did not contain a potential token id
//            System.out.println("found item: " + tokenId + " in inventory.");

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

                if (tokenDisplay != null ) {
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
        // for now, lets assume that we will not be supplying storage boxes with items via the wallet.
        // if we implement chests and crates, this would be a click to expand where items are removed from the temporary
        // container and added to the players main wallet inventory. In the event that we do need to move to a different
        // storage model where chests/boxes/crates are containers -- then we can uncomment the below and alter the logic
        // accordingly.
        // allWalletItems = Arrays.asList(walletInventory.getStorageContents());

        Map<String, ItemStack> walletItems = new HashMap<>();

        for (ItemStack itemStack : allWalletItems) {
            String tokenId = getTokenId(itemStack);

            if (tokenId == null || tokenId.isEmpty()) continue; // this shouldn't happen but just in case...
//            System.out.println("found item: " + tokenId + " in wallet.");

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

                if (tokenDisplay != null ) {
                    walletItems.put(tokenId, clone);
                }
            }
        }

        // okay. now we can compare our two maps and decrement the wallet's available quantities based on the amount current held.
//        for( Map.Entry<String, ItemStack> entry : walletItems.entrySet()) {
//            int remaining = entry.getValue().getAmount() - checkedOutTokens.get(entry.getKey()).getAmount();
//            if (remaining >= 0)
//                entry.getValue().setAmount(remaining);
//            else
//                entry.getValue().setAmount(0); // no more are left to check out.
                // NOTE we'll need to manage remaining items that exceed the available stock.
                // Additionally, we'll need to update the population when a transfer is completed (in either direction)

        for ( String tokenId : wallet.getTokenBalances().keySet()) {
            if ( wallet.getTokenBalances() != null && checkedOutTokens.get(tokenId) != null)
                wallet.getTokenBalances().get(tokenId).setCheckedOut(checkedOutTokens.get(tokenId).getAmount());
        }



        return true;
    }

    public boolean checkoutItem(ItemStack itemStack) {
        String tokenId = getTokenId(itemStack);

        if ( checkedOutTokens.get(tokenId) == null ) {
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
        String tokenId = getTokenId(itemStack);

        if ( checkedOutTokens.get(tokenId) == null ) {
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
