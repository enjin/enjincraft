package io.enjincoin.spigot_framework.listeners;

import io.enjincoin.spigot_framework.BasePlugin;
import io.enjincoin.spigot_framework.util.MessageUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class InventoryListener implements Listener {

    private static final String WALLET_INVENTORY = "Enjin Wallet";

    private BasePlugin main;
    private Map<UUID, List<ItemStack>> checkedOutTokens;

    public InventoryListener(BasePlugin main) {
        this.main = main;
        this.checkedOutTokens = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isWalletInventory(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (isWalletInventory(event.getClickedInventory())) {
            Player player = (Player) event.getClickedInventory().getHolder();
            ItemStack stack = event.getClickedInventory().getItem(event.getSlot());
            event.setCancelled(true);

            /*
            TODO: Use NBT to store custom item metadata for better efficiency.
            See https://github.com/tr7zw/Item-NBT-API
             */

            if (!isCheckedOut(player.getUniqueId(), stack)) {
                List<ItemStack> tokens = checkedOutTokens.get(player.getUniqueId());
                if (tokens == null) {
                    tokens = new ArrayList<>();
                    checkedOutTokens.put(player.getUniqueId(), tokens);
                }

                ItemStack clone = stack.clone();
                ItemMeta meta = clone.getItemMeta();
                List<String> lore = meta.getLore();
                lore.remove(0);
                meta.setLore(lore);
                clone.setItemMeta(meta);

                Map<Integer, ItemStack> result = player.getInventory().addItem(clone);
                if (result.isEmpty()) {
                    tokens.add(clone);
                } else {
                    TextComponent text = TextComponent.of("You do not have sufficient space in your inventory.")
                            .color(TextColor.RED);
                    MessageUtil.sendMessage(player, text);
                }
            } else {
                TextComponent text = TextComponent.of("You have already checked out this item.").color(TextColor.RED);
                MessageUtil.sendMessage(player, text);
            }
        }
    }

    private boolean isWalletInventory(Inventory inventory) {
        if (inventory == null)
            return false;
        return ChatColor.stripColor(inventory.getName()).equalsIgnoreCase(WALLET_INVENTORY);
    }

    private boolean isCheckedOut(UUID uuid, ItemStack stack) {
        List<ItemStack> tokens = checkedOutTokens.get(uuid);
        if (tokens != null && tokens.size() > 0) {
            return tokens.stream().anyMatch(token -> token.getItemMeta().getDisplayName()
                    .equalsIgnoreCase(stack.getItemMeta().getDisplayName()));
        }
        return false;
    }

}
