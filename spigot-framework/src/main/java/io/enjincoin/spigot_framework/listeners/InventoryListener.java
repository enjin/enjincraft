package io.enjincoin.spigot_framework.listeners;

import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import io.enjincoin.spigot_framework.BasePlugin;
import io.enjincoin.spigot_framework.util.MessageUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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
            if (event.getClickedInventory() == null)
                return;

            Player player = (Player) event.getClickedInventory().getHolder();
            ItemStack stack = event.getClickedInventory().getItem(event.getSlot());
            event.setCancelled(true);

            if (stack != null) {
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

                    NBTItem nbt = new NBTItem(clone);
                    nbt.setBoolean("ENJ-Token", true);
                    clone = nbt.getItemStack();

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
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item item = event.getItemDrop();
        if (isCheckedOut(player.getUniqueId(), item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        if (event.getCommand().toLowerCase().startsWith("stop")) {
            clearAll();
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().startsWith("/stop")) {
            clearAll();
        }
    }

    private void clear(Player player) {
        List<ItemStack> stacks = checkedOutTokens.remove(player.getUniqueId());
        PlayerInventory inventory = player.getInventory();

        if (stacks != null) {
            for (ItemStack stack : stacks) {
                inventory.removeItem(stack);
            }
        }
    }

    private void clearAll() {
        Bukkit.getOnlinePlayers().forEach(this::clear);
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
