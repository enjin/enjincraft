package com.enjin.enjincoin.spigot_framework.listeners;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.inventory.WalletCheckoutManager;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import com.enjin.enjincoin.spigot_framework.util.TokenUtils;
import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

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
     * <p>Listener constructor.</p>
     *
     * @param main the Spigot plugin
     */
    public InventoryListener(BasePlugin main) {
        this.main = main;
    }

    /**
     * <p>Subscription to {@link InventoryDragEvent} with the
     * lowest priority.</p>
     *
     * @param event the event
     * @since 1.0
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        main.getLogger().info("InventoryListener#onInventoryDrag");
        if (!(event.getWhoClicked() instanceof Player)) return;

        // TODO: Allow dragging tokenized items only.
        if (isViewingWallet((Player) event.getWhoClicked())) {
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * <p>Subscription to {@link InventoryClickEvent event} with the
     * lowest priority.</p>
     *
     * @param event the event
     * @since 1.0
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        if (!isViewingWallet((Player) event.getWhoClicked())) {
            InventoryView view = event.getWhoClicked().getOpenInventory();
            return;
        }

        if (event.getAction() == InventoryAction.HOTBAR_SWAP ||
                event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setResult(Event.Result.DENY);
        }

        if (event.getAction() == InventoryAction.PLACE_ALL ||
                event.getAction() == InventoryAction.PLACE_ONE ||
                event.getAction() == InventoryAction.PLACE_SOME) {
            // lets validate that the item in question is tagged as a token.
            if (event.getCursor() != null) {
                // check to see if it's a token-tagged nbt
                NBTItem item = new NBTItem(event.getCursor());
                if (item.getString("tokenID") == null)
                    // if it isn't, lets just return and act normally.
                    return;
            }

            // then check to see if we're dropping into the players inventory
            if (!isPlayerInventory(event.getClickedInventory())) {
                event.setCancelled(true);
                return;
            } // else act normally
        }

        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }

        Player player = (Player) event.getClickedInventory().getHolder();
        MinecraftPlayer mcplayer = this.main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId());

        WalletCheckoutManager checkout = mcplayer.getWallet().getCheckoutManager();

        ItemStack cursor = event.getCursor();

        // handle returning an item to the inventory.
        String tokenId = TokenUtils.getTokenID(cursor);

        // check to see if the current cursor stack is a checked out item.
        if (tokenId != null) {
            // System.out.println(tokenId);
            // repair and unbreakable flag the item... just to be safe.
            ItemMeta meta = cursor.getItemMeta();

            if (meta instanceof Damageable) {
                Damageable damageable = (Damageable) meta;
                damageable.setDamage((short) 0);
            }

            meta.setUnbreakable(true);

            cursor.setItemMeta(meta);

            // handle checkout manager return.
            returnItemStack(player, event.getCursor());

            int found = 0;
            // find any instances of the current object in the wallet.
            for (ItemStack stacks : Arrays.asList(event.getClickedInventory().getContents())) {
                String stackId = TokenUtils.getTokenID(stacks);
                if (stackId == null || stackId.isEmpty()) continue;

                if (stackId.equals(tokenId)) {
                    found += stacks.getAmount();
                    if (event.getCurrentItem() == null) { // handle empty slot
                        if (TokenUtils.getTokenID(event.getCurrentItem()).equals(tokenId))
                            event.getClickedInventory().remove(event.getCurrentItem());
                        else
                            event.getClickedInventory().remove(stacks);
                    } else {
                        event.getClickedInventory().remove(stacks);
                    }
                }
            }

            // we found some matching items in the clicked inventory!
            if (found > 0) {
                event.getCursor().setAmount(event.getCursor().getAmount() + found);
            } else { // no matches in the inventory were found
                // noop
            }
            return;
        }

        // handle check out from wallet.
        ItemStack stack = event.getClickedInventory().getItem(event.getSlot());
        event.setCancelled(true);

        Inventory inventory = event.getClickedInventory();
        main.getLogger().info(inventory.getType().name());

        if (stack != null) {
            if (inventory.getType() == InventoryType.PLAYER && TokenUtils.getTokenID(stack) != null) {
                returnItemStack(player, stack);
                event.getClickedInventory().clear(event.getSlot());
            } else if (inventory.getType() == InventoryType.CHEST) {
                if (stack.getAmount() >= 1 || !isCheckedOut(player.getUniqueId(), stack)) {
                    String line = stack.getItemMeta().getLore().get(0).replace("Owned: ", "");
                    line = ChatColor.stripColor(line);
                    int stock = Integer.parseInt(line);
                    ItemStack clone = stack.clone();
                    // only check out 1 item from the wallet at a time (for now!)
                    if (clone.getAmount() >= 1 && stock < 64) {
                        clone.setAmount(1);
                        stack.setAmount(stack.getAmount() - 1);
                    }
                    ItemMeta meta = clone.getItemMeta();
                    meta.setUnbreakable(true);

                    NBTItem nbt = new NBTItem(clone);
                    nbt.setBoolean("ENJ-Token", true);
                    clone = nbt.getItemStack();

                    Map<Integer, ItemStack> result = player.getInventory().addItem(clone);
                    if (result.isEmpty()) {
                        checkout.checkoutItem(clone);
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
     * @since 1.0
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        main.getLogger().info("InventoryListener#onPlayerDropItem");
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
     * @since 1.0
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // If a player quits clear their inventory of all tokens.
        // clear(event.getPlayer());
    }

    /**
     * <p>Subscription to {@link ServerCommandEvent event} with the
     * normal priority.</p>
     *
     * @param event the event
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
        main.getLogger().info("InventoryListener#onInventoryMoveItem");
        if (event.getSource() != null && event.getSource().getHolder() instanceof Player) {
            Player player = (Player) event.getSource().getHolder();
            if (isViewingWallet(player)) {
                Inventory destination = event.getDestination();
                if (destination.getType() == InventoryType.CHEST) {
                    returnItemStack((Player) event.getSource().getHolder(), event.getItem());
                }
            }
        }
    }

    /**
     * <p>Return a (@link ItemStack) to the players wallet inventory.</p>
     *
     * @param player the player
     * @param stack  the ItemStack to return
     * @since 1.0
     */
    private void returnItemStack(Player player, ItemStack stack) {
        MinecraftPlayer mcplayer = this.main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId());
        WalletCheckoutManager manager = mcplayer.getWallet().getCheckoutManager();

        manager.returnItem(stack);
    }

    /**
     * <p>Clear a {@link Player}'s inventory of all checked out tokens.</p>
     *
     * @param player the player
     * @since 1.0
     */
    private void clear(Player player) {
        // TODO reimplement this in the future.
//        List<ItemStack> stacks = checkedOutTokens.remove(player.getUniqueId());
//        PlayerInventory inventory = player.getInventory();
//
//        if (stacks != null) {
//            for (ItemStack stack : stacks) {
//                inventory.removeItem(stack);
//            }
//        }
    }

    /**
     * <p>Clear all players' inventories of all checked out tokens.</p>
     *
     * @since 1.0
     */
    private void clearAll() {
        Bukkit.getOnlinePlayers().forEach(this::clear);
    }

    private boolean isViewingWallet(Player player) {
        if (player != null) {
            InventoryView view = player.getOpenInventory();

            if (view != null) {
                return ChatColor.stripColor(view.getTitle()).equalsIgnoreCase(WALLET_INVENTORY);
            }
        }

        return false;
    }

    /**
     * <p>Check if an inventory represents a valid player inventory.</p>
     *
     * @param inventory the inventory
     * @return true if the inventory represents a valid player inventory
     * @since 1.0
     */
    private boolean isPlayerInventory(Inventory inventory) {
        if (inventory == null)
            return false;
//        return ChatColor.stripColor(inventory.getName()).equalsIgnoreCase(PLAYER_INVENTORY);
        return inventory.getType().equals(InventoryType.PLAYER);
    }


    /**
     * <p>Check if a player has checked out the given {@link ItemStack}
     * from and Enjin wallet.</p>
     *
     * @param uuid  the uuid of the player
     * @param stack the {@link ItemStack}
     * @return true if the {@link ItemStack} represents a checked out token by the player
     * @since 1.0
     */
    private boolean isCheckedOut(UUID uuid, ItemStack stack) {
        MinecraftPlayer mcplayer = this.main.getBootstrap().getPlayerManager().getPlayer(uuid);
        WalletCheckoutManager manager = mcplayer.getWallet().getCheckoutManager();

        String stackId = TokenUtils.getTokenID(stack);

        if (manager.accessCheckout().containsKey(stackId))
            return true;

        return false;
    }

    public static String convertToInvisibleString(String s) {
        String hidden = "";
        for (char c : s.toCharArray()) hidden += ChatColor.COLOR_CHAR + "" + c;
        return hidden;
    }
}
