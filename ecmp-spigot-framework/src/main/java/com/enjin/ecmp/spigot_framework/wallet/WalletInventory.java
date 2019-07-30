package com.enjin.ecmp.spigot_framework.wallet;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.ecmp.spigot_framework.player.MinecraftPlayer;
import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletInventory {

    public static MinecraftPlayer owner;
    public static Map<String, ItemStack> items;

    private static final int MAX_STACK_SIZE = 64;

    public static Inventory create(BasePlugin main, Player holder, List<MutableBalance> tokens) {
        // initialize item map if not already initialized
        if (items == null)
            items = new HashMap<>();

        // Fetch configured tokens from the config.
        JsonObject tokensDisplayConfig = main.getBootstrap().getConfig().getRoot().get("tokens").getAsJsonObject();
        // Generate a 6 by 9 chest inventory.
        Inventory inventory = Bukkit.createInventory(holder, 6 * 9, ChatColor.DARK_PURPLE + "Enjin Wallet");
        inventory.setMaxStackSize(MAX_STACK_SIZE);

        owner = main.getBootstrap().getPlayerManager().getPlayer(holder.getUniqueId());
        owner.getWallet().setInventory(inventory);
        WalletCheckoutManager manager = owner.getWallet().getCheckoutManager();

        int index = 0;
        for (MutableBalance entry : tokens) {
            // Check if the inventory is full.
            if (index >= 6 * 9) {
                break;
            }

            // Check if the token entry has value.
            if (entry.balance().compareTo(0) == 1) {
                // Fetch data for the matching token ID.
                JsonObject tokenDisplay = tokensDisplayConfig.has(String.valueOf(entry.id()))
                        ? tokensDisplayConfig.get(String.valueOf(entry.id())).getAsJsonObject()
                        : null;

                if (tokenDisplay == null) continue;

                // Select a material to use for this menu entry.
                Material material = null;
                if (tokenDisplay.has("material"))
                    material = Material.getMaterial(tokenDisplay.get("material").getAsString());
                if (material == null)
                    material = Material.APPLE;

                // Create an ItemStack with the selected material.
                ItemStack stack = new ItemStack(material);
                // deduct checked out item counts from the balance available
                int amount = entry.balance().intValue();
                if (manager.accessCheckout().isEmpty()) {
                    manager.populate(main, owner.getBukkitPlayer(), owner.getWallet());
                }
                if (manager.accessCheckout().get(entry.id()) != null)
                    amount -= manager.accessCheckout().get(entry.id()).getAmount();

                stack.setAmount(amount);

                ItemMeta meta = stack.getItemMeta();
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

                // Set the display name to provided display name or generate one if not specified.
                if (tokenDisplay != null && tokenDisplay.has("displayName")) {
                    meta.setDisplayName(ChatColor.DARK_PURPLE + tokenDisplay.get("displayName").getAsString());
                } else {
                    // Use token ID as display name.
                    meta.setDisplayName(ChatColor.DARK_PURPLE + "Token #" + entry.id());
                }

                List<String> lore = new ArrayList<>();
                // Add balance to lore.
                lore.add(ChatColor.GRAY + "Owned: " + ChatColor.GOLD + entry.balance().intValue());

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

                NBTItem nbti = new NBTItem(stack);
                nbti.setString("tokenID", entry.id());

                // Add ItemStack to wallet inventory.
                inventory.setItem(index++, nbti.getItemStack());

                // Add ItemStack to local map.
                items.put(holder.getName(), nbti.getItemStack());
            }

        }
        return inventory;
    }

}
