package com.enjin.enjincoin.spigot_framework.listeners;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.inventory.WalletCheckoutManager;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import com.enjin.enjincoin.spigot_framework.util.TokenUtils;
import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class InventoryListener implements Listener {

    private static final String WALLET_INVENTORY = "Enjin Wallet";

    private BasePlugin plugin;

    public InventoryListener(BasePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        // TODO: Allow dragging tokenized items only.
        if (isViewingWallet((Player) event.getWhoClicked())) {
            event.setResult(Event.Result.DENY);
        }
    }

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
        MinecraftPlayer mcplayer = this.plugin.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId());

        WalletCheckoutManager checkout = mcplayer.getWallet().getCheckoutManager();

        ItemStack cursor = event.getCursor();

        // handle returning an item to the inventory.
        String tokenId = TokenUtils.getTokenID(cursor);

        // check to see if the current cursor stack is a checked out item.
        if (tokenId != null) {
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

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // If a player drops a token cancel the event.
        Player player = event.getPlayer();
        Item item = event.getItemDrop();

        if (isCheckedOut(player.getUniqueId(), item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
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

    private void returnItemStack(Player player, ItemStack stack) {
        MinecraftPlayer mcplayer = this.plugin.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId());
        WalletCheckoutManager manager = mcplayer.getWallet().getCheckoutManager();

        manager.returnItem(stack);
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

    private boolean isPlayerInventory(Inventory inventory) {
        if (inventory == null)
            return false;
        return inventory.getType().equals(InventoryType.PLAYER);
    }

    private boolean isCheckedOut(UUID uuid, ItemStack stack) {
        MinecraftPlayer mcplayer = this.plugin.getBootstrap().getPlayerManager().getPlayer(uuid);
        WalletCheckoutManager manager = mcplayer.getWallet().getCheckoutManager();

        String stackId = TokenUtils.getTokenID(stack);

        if (manager.accessCheckout().containsKey(stackId))
            return true;

        return false;
    }
}
