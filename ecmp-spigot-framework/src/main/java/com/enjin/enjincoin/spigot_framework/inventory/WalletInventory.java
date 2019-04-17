package com.enjin.enjincoin.spigot_framework.inventory;

import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import com.enjin.enjincoin.sdk.model.service.tokens.Token;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.player.TokenData;
import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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

    private static final int maxStackSize = 64;

    /**
     * <p>Returns an {@link Inventory} representing an Enjin wallet
     * for the provided {@link InventoryHolder} and {@link Identity}.</p>
     *
     * @param main   the Spigot plugin
     * @param holder the inventory holder
     * @param tokens the identity
     * @return an inventory that represents an Enjin wallet
     * @since 1.0
     */
    public static Inventory create(BasePlugin main, Player holder, List<TokenData> tokens) {
        // initialize item map if not already initialized
        if (items == null)
            items = new HashMap<>();

//        System.out.println("WalletInventory.create: started");

        // Fetch configured tokens from the config.
        JsonObject tokensDisplayConfig = main.getBootstrap().getConfig().get("tokens").getAsJsonObject();
        // Generate a 6 by 9 chest inventory.
        Inventory inventory = Bukkit.createInventory(holder, 6 * 9, ChatColor.DARK_PURPLE + "Enjin Wallet");
        inventory.setMaxStackSize(maxStackSize);

        // TODO this may not be necessary .. will ask Tim or Evan
        owner = main.getBootstrap().getPlayerManager().getPlayer(holder.getUniqueId());
        owner.getWallet().setInventory(inventory);
        WalletCheckoutManager manager = owner.getWallet().accessCheckoutManager();

//        System.out.println("WalletInventory.create: setup checkout manager");

        int index = 0;
//        System.out.println("WalletInventory.create: processing " + tokens.size() + " tokens");
        for (TokenData entry : tokens) {
            // Check if the inventory is full.
            if (index >= 6 * 9) {
//                System.out.println("WalletInventory.create: failed at index...");
                break;
            }

            // Check if the token entry has value.
//            System.out.println("WalletInventory.create: entry.getBalance() " + entry.getBalance());
            if (entry.getBalance() > 0) {
                //TODO if the balance is greater than maxStackSize, split the item into multiple stacks in the Wallet Inventory
//                System.out.println("WalletInventory.create: entry is " + entry.getId());
                Token token = main.getBootstrap().getTokens().get(entry.getId());
                // Verify that the token data exists.

                if (token == null) {
//                    System.out.println("WalletInventory.create: token is null for entry id " + entry.getId());
                    continue;
                }
//                System.out.println("WalletInventory.create: token is " + token.getTokenId());

                // Fetch data for the matching token ID.
                JsonObject tokenDisplay = tokensDisplayConfig.has(String.valueOf(token.getTokenId()))
                        ? tokensDisplayConfig.get(String.valueOf(token.getTokenId())).getAsJsonObject()
                        : null;

                // Select a material to use for this menu entry.
                Material material = null;
                if (tokenDisplay != null && tokenDisplay.has("material"))
                    material = Material.getMaterial(tokenDisplay.get("material").getAsString());
                if (material == null)
                    material = Material.APPLE;

                // Create an ItemStack with the selected material.
                ItemStack stack = new ItemStack(material);
                // deduct checked out item counts from the amount available
                int amount = entry.getBalance().intValue();
                if (manager.accessCheckout().isEmpty()) {
                    manager.populate(main, owner.getBukkitPlayer(), owner.getWallet());
                }
                if (manager.accessCheckout().get(entry.getId()) != null)
                    amount -= manager.accessCheckout().get(entry.getId()).getAmount();

                stack.setAmount(amount);
                // TODO re-evaluate the unbreakable status for ENJ backed items.
                stack.getItemMeta().setUnbreakable(true);

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
                double balance = stack.getAmount();
                lore.add(ChatColor.GRAY + "Owned: " + ChatColor.GOLD + entry.getBalance().intValue());

                // Fetch and use lore description if found.
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

                // TODO work with stackable property, like stash this in NBT
//                boolean stacks = false;
//                if (tokenDisplay != null && tokenDisplay.has("stacks")) {
//                    stacks = tokenDisplay.get("stacks").getAsBoolean();
//                }

                // Replace the meta's lore.
                meta.setLore(lore);
                stack.setItemMeta(meta);

                NBTItem nbti = new NBTItem(stack);
                nbti.setString("tokenID", token.getTokenId());

//                System.out.println("WalletInventory.create: created NBT for item " + token.getTokenId());

                // Add ItemStack to wallet inventory.
//                inventory.setItem(index++, stack);
                inventory.setItem(index++, nbti.getItemStack());

                System.out.println("WalletInventory.create: added stack to inventory");

                // Add ItemStack to local map.
//                items.put(holder.getName(), stack);
                items.put(holder.getName(), nbti.getItemStack());
                System.out.println("WalletInventory.create: added item to local map");
            } else {
                System.out.println("WalletInventory.create: entry.balance was less than 1...");
            }

        }
        System.out.println("WalletInventory.create: done. returning inventory!");
        return inventory;
    }

    /**
     * <p>Returns an {@link Inventory} representing an Enjin wallet
     * for the provided {@link InventoryHolder} and {@link Identity}.</p>
     *
     * @param main     the Spigot plugin
     * @param holder   the inventory holder
     * @param identity the identity
     * @return an inventory that represents an Enjin wallet
     * @since 1.0
     */
    public static Inventory create(BasePlugin main, InventoryHolder holder, Identity identity) {
        // Fetch configured tokens from the config.
        JsonObject tokensDisplayConfig = main.getBootstrap().getConfig().get("tokens").getAsJsonObject();
        // Generate a 6 by 9 chest inventory.
        Inventory inventory = Bukkit.createInventory(holder, 6 * 9, ChatColor.DARK_PURPLE + "Enjin Wallet");
        inventory.setMaxStackSize(maxStackSize);

        int index = 0;
        for (Token entry : identity.getTokens()) {
            // Check if the inventory is full.
            if (index >= 6 * 9)
                break;

            // Check if the token entry has value.
            if (entry.getBalance() > 0) {
                //TODO if the balance is greater than maxStackSize, split the item into multiple stacks in the Wallet Inventory
                Token token = main.getBootstrap().getTokens().get(entry.getTokenId());
                // Verify that the token data exists.
                if (token == null)
                    continue;

                // Fetch data for the matching token ID.
                JsonObject tokenDisplay = tokensDisplayConfig.has(String.valueOf(token.getTokenId()))
                        ? tokensDisplayConfig.get(String.valueOf(token.getTokenId())).getAsJsonObject()
                        : null;

//                if (tokenDisplay == null)
//                    System.out.println("tokenDisplay is null");

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
                double balance = Double.valueOf(entry.getBalance().intValue());
                lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GOLD + balance);
                lore.add(ChatColor.GRAY + "ENJ Melt Value: " + ChatColor.GOLD + entry.getMeltValue());

                // Fetch and use lore description if found.
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

    public static String convertToInvisibleString(String s) {
        String hidden = "";
        for (char c : s.toCharArray()) hidden += ChatColor.COLOR_CHAR + "" + c;
        return hidden;
    }
}
