package io.enjincoin.spigot_framework.listeners;

import io.enjincoin.spigot_framework.BasePlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class InventoryListener implements Listener {

    private static final String WALLET_INVENTORY = "Enjin Wallet";

    private BasePlugin main;

    public InventoryListener(BasePlugin main) {
        this.main = main;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isWalletInventory(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (isWalletInventory(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    private boolean isWalletInventory(Inventory inventory) {
        return ChatColor.stripColor(inventory.getName()).equalsIgnoreCase(WALLET_INVENTORY);
    }

}
