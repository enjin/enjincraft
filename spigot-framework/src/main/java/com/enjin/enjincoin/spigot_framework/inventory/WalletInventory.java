package com.enjin.enjincoin.spigot_framework.inventory;

import com.enjin.enjincoin.spigot_framework.listeners.InventoryListener;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.player.TokenData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.sdk.client.service.identities.vo.data.IdentitiesData;
import com.enjin.enjincoin.sdk.client.service.tokens.vo.Token;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Generates Enjin wallet inventories for online players
 * with an associated identity.</p>
 *
 * @since 1.0
 */
public class WalletInventory {

    /**
     * <p>Price display format for tokens.</p>
     */
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    public static MinecraftPlayer owner;
    public static Map<String, ItemStack> items;

    /**
     * <p>Returns an {@link Inventory} representing an Enjin wallet
     * for the provided {@link InventoryHolder} and {@link Identity}.</p>
     *
     * @param main the Spigot plugin
     * @param holder the inventory holder
     * @param tokens the identity
     *
     * @return an inventory that represents an Enjin wallet
     *
     * @since 1.0
     */
    public static Inventory create(BasePlugin main, Player holder, List<TokenData> tokens) {
        // initialize item map if not already initialized
        if (items == null)
            items = new HashMap<>();

        // Fetch configured tokens from the config.
        JsonObject tokensDisplayConfig = main.getBootstrap().getConfig().get("tokens").getAsJsonObject();
        // Generate a 6 by 9 chest inventory.
        Inventory inventory = Bukkit.createInventory(holder, 6 * 9, ChatColor.DARK_PURPLE + "Enjin Wallet");

        // TODO this may not be necessary .. will ask Tim or Evan
        owner = main.getBootstrap().getPlayerManager().getPlayer(holder.getUniqueId());
        owner.getWallet().setInventory(inventory);

        int index = 0;
        for (TokenData entry : tokens) {
            // Check if the inventory is full.
            if (index >= 6 * 9)
                break;

            // Check if the token entry has value.
            if (entry.getBalance() > 0) {
                Token token = main.getBootstrap().getTokens().get(entry.getId());
                // Verify that the token data exists.
                if (token == null)
                    continue;

                // Fetch data for the matching token ID.
                JsonObject tokenDisplay = tokensDisplayConfig.has(String.valueOf(token.getTokenId()))
                        ? tokensDisplayConfig.get(String.valueOf(token.getTokenId())).getAsJsonObject()
                        : null;

                if (tokenDisplay == null)
                    System.out.println("tokenDisplay is null");

                // Select a material to use for this menu entry.
                Material material = null;
                if (tokenDisplay != null && tokenDisplay.has("material"))
                    material = Material.getMaterial(tokenDisplay.get("material").getAsString());
                if (material == null)
                    material = Material.APPLE;

                // Create an ItemStack with the selected material.
                ItemStack stack = new ItemStack(material);
                stack.setAmount(entry.getBalance().intValue());

                ItemMeta meta = stack.getItemMeta();

                // Set the display name to provided display name or generate one if not specified.
                if (tokenDisplay != null && tokenDisplay.has("displayName")) {
                    meta.setDisplayName(ChatColor.DARK_PURPLE + tokenDisplay.get("displayName").getAsString());
                } else {
                    if (token.getName() != null)
                        // Use token name as display name.
                        meta.setDisplayName(ChatColor.DARK_PURPLE + token.getName());
                    else
                        // Use token ID as display name.
                        meta.setDisplayName(ChatColor.DARK_PURPLE + "Token #" + token.getTokenId());
                }

                List<String> lore = new ArrayList<>();
                // Add balance to lore.
                if (token.getDecimals() != null && token.getDecimals() == 0) {
                    // Display balance as an integer.
                    double balance = stack.getAmount();
                    lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GOLD + balance);
//                    lore.add(ChatColor.GRAY + "ENJ Melt Value: " + ChatColor.GOLD + entry.getMeltValue() );
                } else {
//                    // Display balance using the price format.
                    double balance = entry.getBalance();
                    lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GOLD + DECIMAL_FORMAT.format(balance));
                }

//              // Fetch and use lore description if found.
                if (tokenDisplay != null && tokenDisplay.has("lore")) {
                    JsonElement element = tokenDisplay.get("lore");
                    if (element.isJsonArray()) {
                        // Add each element of the array as a line of lore.
                        JsonArray array = element.getAsJsonArray();
                        for (JsonElement line : array) {
                            lore.add(ChatColor.DARK_GRAY + line.getAsString());
                        }
                    } else {
                        // Add string as a line of lore.
                        lore.add(ChatColor.DARK_GRAY + element.getAsString());
                    }
                }

                // Replace the meta's lore.
                meta.setLore(lore);
                stack.setItemMeta(meta);

                // Add ItemStack to wallet inventory.
                inventory.setItem(index++, stack);

                // Add ItemStack to local map.
                items.put(holder.getName(), stack);
            }
        }
        return inventory;
    }

    /**
     * <p>Returns an {@link Inventory} representing an Enjin wallet
     * for the provided {@link InventoryHolder} and {@link Identity}.</p>
     *
     * @param main the Spigot plugin
     * @param holder the inventory holder
     * @param identity the identity
     *
     * @return an inventory that represents an Enjin wallet
     *
     * @since 1.0
     */
    public static Inventory create(BasePlugin main, InventoryHolder holder, Identity identity) {
        // Fetch configured tokens from the config.
        JsonObject tokensDisplayConfig = main.getBootstrap().getConfig().get("tokens").getAsJsonObject();
        // Generate a 6 by 9 chest inventory.
        Inventory inventory = Bukkit.createInventory(holder, 6 * 9, ChatColor.DARK_PURPLE + "Enjin Wallet");

        int index = 0;
        for (Token entry : identity.getTokens()) {
            // Check if the inventory is full.
            if (index >= 6 * 9)
                break;

            // Check if the token entry has value.
            if (entry.getBalance() > 0) {
                Token token = main.getBootstrap().getTokens().get(entry.getTokenId());
                // Verify that the token data exists.
                if (token == null)
                    continue;

                // Fetch data for the matching token ID.
                JsonObject tokenDisplay = tokensDisplayConfig.has(String.valueOf(token.getTokenId()))
                        ? tokensDisplayConfig.get(String.valueOf(token.getTokenId())).getAsJsonObject()
                        : null;

                if (tokenDisplay == null)
                    System.out.println("tokenDisplay is null");

                // Select a material to use for this menu entry.
                Material material = null;
                if (tokenDisplay != null && tokenDisplay.has("material"))
                    material = Material.getMaterial(tokenDisplay.get("material").getAsString());
                if (material == null)
                    material = Material.APPLE;

                // Create an ItemStack with the selected material.
                ItemStack stack = new ItemStack(material);
                stack.setAmount(entry.getBalance().intValue());

                ItemMeta meta = stack.getItemMeta();

                // Set the display name to provided display name or generate one if not specified.
                if (tokenDisplay != null && tokenDisplay.has("displayName")) {
                    meta.setDisplayName(ChatColor.DARK_PURPLE + tokenDisplay.get("displayName").getAsString());
                } else {
                    if (token.getName() != null)
                        // Use token name as display name.
                        meta.setDisplayName(ChatColor.DARK_PURPLE + token.getName());
                    else
                        // Use token ID as display name.
                        meta.setDisplayName(ChatColor.DARK_PURPLE + "Token #" + token.getTokenId());
                }

                List<String> lore = new ArrayList<>();
                // Add balance to lore.
                if (token.getDecimals() != null && token.getDecimals() == 0) {
                    // Display balance as an integer.
                    double balance = Double.valueOf(entry.getBalance().intValue());
                    lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GOLD + balance);
                    lore.add(ChatColor.GRAY + "ENJ Melt Value: " + ChatColor.GOLD + entry.getMeltValue() );
                } else {
//                    // Display balance using the price format.
                    double balance = entry.getBalance();
                    lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GOLD + DECIMAL_FORMAT.format(balance));
                }
//
//                // Fetch and use lore description if found.
                if (tokenDisplay != null && tokenDisplay.has("lore")) {
                    JsonElement element = tokenDisplay.get("lore");
                    if (element.isJsonArray()) {
                        // Add each element of the array as a line of lore.
                        JsonArray array = element.getAsJsonArray();
                        for (JsonElement line : array) {
                            lore.add(ChatColor.DARK_GRAY + line.getAsString());
                        }
                    } else {
                        // Add string as a line of lore.
                        lore.add(ChatColor.DARK_GRAY + element.getAsString());
                    }
                }

                // Replace the meta's lore.
                meta.setLore(lore);
                stack.setItemMeta(meta);

                // Add ItemStack to wallet inventory.
                inventory.setItem(index++, stack);
            }
        }
        return inventory;
    }
}
