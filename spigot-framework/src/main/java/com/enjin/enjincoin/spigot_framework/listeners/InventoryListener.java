package com.enjin.enjincoin.spigot_framework.listeners;

import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
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
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * <p>A listener for handling events for which queries and updates to
 * an {@link Player}'s inventory take place.</p>
 *
 * @since 1.0
 */
public class InventoryListener implements Listener {

    /**
     * <p>The name of an inventory representing an Enjin wallet.</p>
     */
    private static final String WALLET_INVENTORY = "Enjin Wallet";

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    /**
     * <p>A mapping of {@link ItemStack}s representing tokens checked
     * out by an online player.</p>
     */
    private Map<UUID, List<ItemStack>> checkedOutTokens;

    /**
     * <p>Listener constructor.</p>
     *
     * @param main the Spigot plugin
     */
    public InventoryListener(BasePlugin main) {
        this.main = main;
        this.checkedOutTokens = new HashMap<>();
    }

    /**
     * <p>Subscription to {@link InventoryDragEvent} with the
     * lowest priority.</p>
     *
     * @param event the event
     *
     * @since 1.0
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        /*
        Check if the inventory the player dragged in is an
        instance of a wallet inventory.
         */
        if (isWalletInventory(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    /**
     * <p>Subscription to {@link InventoryClickEvent event} with the
     * lowest priority.</p>
     *
     * @param event the event
     *
     * @since 1.0
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (isWalletInventory(event.getClickedInventory())) {
            if (event.getClickedInventory() == null)
                return;

            Player player = (Player) event.getClickedInventory().getHolder();
            ItemStack stack = event.getClickedInventory().getItem(event.getSlot());
            event.setCancelled(true);

            if (stack != null) {
                if (stack.getAmount() >= 1 || !isCheckedOut(player.getUniqueId(), stack)) {
                    List<ItemStack> tokens = checkedOutTokens.get(player.getUniqueId());
                    if (tokens == null) {
                        tokens = new ArrayList<>();
                        checkedOutTokens.put(player.getUniqueId(), tokens);
                    }

                    ItemStack clone = stack.clone();
                    // only check out 1 item from the wallet at a time (for now!)
                    int balance = 1;
                    if (clone.getAmount() >= 1) {
                        balance = clone.getAmount() - 1;
                        clone.setAmount(1);
                        stack.setAmount(balance);
                        ItemMeta meta = stack.getItemMeta();
                        List<String> lore = meta.getLore();
//                        lore.set(1, ChatColor.GRAY + "Available: " + ChatColor.GOLD + balance);
                    }
                    ItemMeta meta = clone.getItemMeta();
                    List<String> lore = meta.getLore();
                    lore.set(0,  ChatColor.GRAY + "Item checked out from ENJ Wallet."); // remove Owned line
                    lore.remove(1); // remove Available line
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
                        MessageUtils.sendMessage(player, text);
                    }
                } else {
                    TextComponent text = TextComponent.of("You have already checked out this item.").color(TextColor.RED);
                    MessageUtils.sendMessage(player, text);
                }
            }
        }
    }

    /**
     * <p>Subscription to {@link PlayerDropItemEvent event} with the
     * normal priority.</p>
     *
     * @param event the event
     *
     * @since 1.0
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // If a player drops a token cancel the event.
        Player player = event.getPlayer();
        Item item = event.getItemDrop();

        if (isCheckedOut(player.getUniqueId(), item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    /**
     * <p>Subscription to {@link PlayerQuitEvent event} with the
     * normal priority.</p>
     *
     * @param event the event
     *
     * @since 1.0
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // If a player quits clear their inventory of all tokens.
        clear(event.getPlayer());
    }

    /**
     * <p>Subscription to {@link ServerCommandEvent event} with the
     * normal priority.</p>
     *
     * @param event the event
     *
     * @since 1.0
     */
    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        /*
        If the console operator runs the 'stop' command clear
        all tokens from every online player's inventory.
         */
        if (event.getCommand().toLowerCase().startsWith("stop")) {
            clearAll();
        }
    }

    /**
     * <p>Subscription to {@link PlayerCommandPreprocessEvent event} with the
     * normal priority.</p>
     *
     * @param event the event
     *
     * @since 1.0
     */
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        /*
        If a player runs the '/stop' command clear
        all tokens from every online player's inventory.
         */
        if (event.getMessage().startsWith("/stop")) {
            clearAll();
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getDestination().getName().equalsIgnoreCase("enjin wallet")) {
            returnItemStack((Player)event.getSource().getHolder(), event.getItem());
        }
        event.getItem().setAmount(0);
    }

    /**
     * <p>Return a (@link ItemStack) to the players wallet inventory.</p>
     *
     * @param player the player
     * @param stack the ItemStack to return
     *
     * @since 1.0
     */
    private void returnItemStack(Player player, ItemStack stack) {
        List<ItemStack> stacks = checkedOutTokens.get(player.getUniqueId());
        if (stacks.contains(stack))
            checkedOutTokens.get(player.getUniqueId()).remove(stack);

        int idx = main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId()).getWallet().getInventory().first(stack);
        int balance = main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId()).getWallet().getInventory().getItem(idx).getAmount() + stack.getAmount();
        main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId()).getWallet().getInventory().getItem(idx).setAmount(balance);
        main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId()).getWallet().getInventory().getItem(idx).getItemMeta().getLore().set(0, ChatColor.GRAY + "Balance: " + ChatColor.GOLD + balance);
    }

    /**
     * <p>Clear a {@link Player}'s inventory of all checked out tokens.</p>
     *
     * @param player the player
     *
     * @since 1.0
     */
    private void clear(Player player) {
        List<ItemStack> stacks = checkedOutTokens.remove(player.getUniqueId());
        PlayerInventory inventory = player.getInventory();

        if (stacks != null) {
            for (ItemStack stack : stacks) {
                inventory.removeItem(stack);
            }
        }
    }

    /**
     * <p>Clear all players' inventories of all checked out tokens.</p>
     *
     * @since 1.0
     */
    private void clearAll() {
        Bukkit.getOnlinePlayers().forEach(this::clear);
    }

    /**
     * <p>Check if an inventory represents an Enjin wallet.</p>
     *
     * @param inventory the inventory
     *
     * @return true if the inventory represents an Enjin wallet
     *
     * @since 1.0
     */
    private boolean isWalletInventory(Inventory inventory) {
        if (inventory == null)
            return false;
        return ChatColor.stripColor(inventory.getName()).equalsIgnoreCase(WALLET_INVENTORY);
    }

    /**
     * <p>Check if a player has checked out the given {@link ItemStack}
     * from and Enjin wallet.</p>
     *
     * @param uuid the uuid of the player
     * @param stack the {@link ItemStack}
     *
     * @return true if the {@link ItemStack} represents a checked out token by the player
     *
     * @since 1.0
     */
    private boolean isCheckedOut(UUID uuid, ItemStack stack) {
        List<ItemStack> tokens = checkedOutTokens.get(uuid);
        if (tokens != null && tokens.size() > 0) {
            return tokens.stream().anyMatch(token -> token.getItemMeta().getDisplayName()
                    .equalsIgnoreCase(stack.getItemMeta().getDisplayName()));
        }
        return false;
    }

}
