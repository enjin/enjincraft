package com.enjin.enjincraft.spigot.listeners;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.wallet.MutableBalance;
import com.enjin.enjincraft.spigot.wallet.TokenWallet;
import com.enjin.minecraft_commons.spigot.ui.AbstractMenu;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;

import java.util.Map;

public class TokenItemListener implements Listener {

    private final SpigotBootstrap bootstrap;

    public TokenItemListener(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory())
            return;

        try {
            EnjPlayer enjPlayer = bootstrap.getPlayerManager()
                    .getPlayer(event.getEntity());
            if (enjPlayer == null)
                return;

            TokenWallet wallet = enjPlayer.getTokenWallet();

            for (ItemStack is : event.getDrops()) {
                if (!TokenUtils.hasTokenData(is)) {
                    continue;
                } else if (!TokenUtils.isValidTokenItem(is)) {
                    is.setAmount(0);
                    bootstrap.debug(String.format("Removed corrupted token from %s when they died", event.getEntity().getDisplayName()));
                    continue;
                }

                MutableBalance balance = wallet.getBalance(TokenUtils.getTokenID(is),
                                                           TokenUtils.getTokenIndex(is));
                balance.deposit(is.getAmount());
                is.setAmount(0);
            }
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack is = event.getItemDrop().getItemStack();
        if (!TokenUtils.hasTokenData(is)) {
            return;
        } else if (!TokenUtils.isValidTokenItem(is)) {
            is.setAmount(0);
            bootstrap.debug(String.format("Removed corrupted token when %s dropped it", event.getPlayer().getDisplayName()));
            return;
        }

        EnjPlayer enjPlayer = bootstrap.getPlayerManager()
                .getPlayer(event.getPlayer());
        if (enjPlayer == null) {
            is.setAmount(0);
            bootstrap.debug(String.format("Removed token from non-Enjin player when %s dropped it", event.getPlayer().getDisplayName()));
            return;
        }

        TokenWallet wallet = enjPlayer.getTokenWallet();
        if (wallet == null) {
            is.setAmount(0);
            bootstrap.debug(String.format("Removed token from unlinked Enjin player when %s dropped it", event.getPlayer().getDisplayName()));
            return;
        }

        event.setCancelled(true); // Will cancel event due to valid token being detected for valid player

        // Checks for available space
        PlayerInventory inventory = event.getPlayer().getInventory();
        int             size      = inventory.getSize()
                                    - (inventory.getArmorContents().length + inventory.getExtraContents().length);
        ItemStack dropItem = null;
        int       dropSlot = -1;
        for (int i = 0; i < size; i++) {
            ItemStack item  = inventory.getItem(i);
            if (item == null
                    || item.getType() == Material.AIR
                    || TokenUtils.canCombineStacks(is, item)) {
                return;
            } else if (dropItem == null && !TokenUtils.hasTokenData(item)) {
                dropSlot = i;
                dropItem = item;
            }
        }

        // Returns token to wallet if inventory cannot be changed
        if (dropItem == null) {
            try {
                MutableBalance balance = wallet.getBalance(TokenUtils.getTokenID(is),
                                                           TokenUtils.getTokenIndex(is));
                balance.deposit(is.getAmount());
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }
        }

        Player player = enjPlayer.getBukkitPlayer();
        player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
        inventory.clear(dropSlot);
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();
        if (AbstractMenu.hasAnyMenu(player))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getView().getPlayer();
        if (AbstractMenu.hasAnyMenu(player))
            return;

        ItemStack clk = event.getCurrentItem();
        ItemStack held = event.getCursor();
        if (held != null && held.getType() != Material.AIR)
            inventoryClickHolding(event, held);
        else if (clk != null && clk.getType() != Material.AIR)
            inventoryClickClicked(event, clk);
    }

    private void inventoryClickClicked(InventoryClickEvent event, ItemStack is) {
        if (!TokenUtils.hasTokenData(is))
            return;

        Inventory clkInv = event.getClickedInventory();
        if (!(clkInv instanceof PlayerInventory) || event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return;
        } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            Inventory top = event.getView().getTopInventory();

            /* Condition is associated with the standard player inventory view which allows the player to use
             * hot-key to move/equip items within their own inventory. The top inventory is assumed to be the
             * 2x2 crafting menu.
             */
            if (top instanceof CraftingInventory && top.getSize() == 5)
                return;
        }

        event.setCancelled(true);
    }

    private void inventoryClickHolding(InventoryClickEvent event, ItemStack is) {
        if (!TokenUtils.hasTokenData(is))
            return;

        Inventory clkInv = event.getClickedInventory();
        if (clkInv instanceof PlayerInventory)
            return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryView view   = event.getView();
        Player        player = (Player) view.getPlayer();
        if (AbstractMenu.hasAnyMenu(player))
            return;

        for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            Integer   slot = entry.getKey();
            ItemStack is   = entry.getValue();
            if (view.getInventory(slot) != player.getInventory() && TokenUtils.hasTokenData(is)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        for (ItemStack is : event.getInventory()) {
            if (TokenUtils.hasTokenData(is)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent event) {
        ItemStack held = event.getPlayerItem();
        if (TokenUtils.hasTokenData(held))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof ItemFrame)
            interactItemFrame(event, (ItemFrame) entity);
    }

    private void interactItemFrame(PlayerInteractEntityEvent event, ItemFrame itemFrame) {
        ItemStack held = event.getPlayer()
                .getInventory()
                .getItemInMainHand();
        if (itemFrame.getItem().getType() == Material.AIR && TokenUtils.hasTokenData(held))
            event.setCancelled(true);
    }

    @EventHandler
    public void onBucketEmptyEvent(PlayerBucketEmptyEvent event) {
        handlePlayerBucketEvent(event);
    }

    @EventHandler
    public void onBucketFillEvent(PlayerBucketFillEvent event) {
        handlePlayerBucketEvent(event);
    }

    private void handlePlayerBucketEvent(PlayerBucketEvent event) {
        PlayerInventory inventory = event.getPlayer().getInventory();
        ItemStack mainHand = inventory.getItemInMainHand();
        ItemStack offHand  = inventory.getItemInOffHand();
        ItemStack bucket;

        // Minecraft prioritizes buckets in the main hand
        if (isBucket(mainHand))
            bucket = mainHand;
        else if (isBucket(offHand))
            bucket = offHand;
        else
            return;

        if (TokenUtils.hasTokenData(bucket))
            event.setCancelled(true);
    }

    private static boolean isBucket(ItemStack is) {
        if (is == null)
            return false;

        switch (is.getType()) {
            case BUCKET:
            case WATER_BUCKET:
            case LAVA_BUCKET:
            case MILK_BUCKET:
            case COD_BUCKET:
            case PUFFERFISH_BUCKET:
            case SALMON_BUCKET:
            case TROPICAL_FISH_BUCKET:
                return true;
            default:
                return false;
        }
    }

}
