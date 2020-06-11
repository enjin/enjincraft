package com.enjin.enjincraft.spigot.listeners;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.StringUtils;
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

import java.util.List;
import java.util.Map;

public class TokenItemListener implements Listener {

    private SpigotBootstrap bootstrap;

    public TokenItemListener(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory())
            return;

        try {
            EnjPlayer enjPlayer = bootstrap.getPlayerManager()
                    .getPlayer(event.getEntity())
                    .orElse(null);
            if (enjPlayer == null)
                return;

            TokenWallet wallet = enjPlayer.getTokenWallet();
            List<ItemStack> drops = event.getDrops();

            for (int i = drops.size() - 1; i >= 0; i--) {
                ItemStack is    = drops.get(i);
                String    id    = TokenUtils.getTokenID(is);
                String    index = TokenUtils.getTokenIndex(is);
                if (StringUtils.isEmpty(id) || StringUtils.isEmpty(index))
                    continue;

                MutableBalance balance = wallet.getBalance(id, index);
                balance.deposit(is.getAmount());
            }
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        EnjPlayer enjPlayer = bootstrap.getPlayerManager()
                .getPlayer(event.getPlayer())
                .orElse(null);
        ItemStack is    = event.getItemDrop().getItemStack();
        String    id    = TokenUtils.getTokenID(is);
        String    index = TokenUtils.getTokenIndex(is);
        if (enjPlayer == null || StringUtils.isEmpty(id) || StringUtils.isEmpty(index))
            return;

        event.setCancelled(true); // Will cancel event due to token being detected

        // Checks for available space
        PlayerInventory inventory = event.getPlayer().getInventory();
        int size = inventory.getSize() - (inventory.getArmorContents().length + inventory.getExtraContents().length);
        int slot = -1;
        for (int i = 0; i < size; i++) {
            ItemStack item   = inventory.getItem(i);
            String    itemId = TokenUtils.getTokenID(item);
            if (itemId == null || TokenUtils.canCombineStacks(is, item))
                return;
            else if (StringUtils.isEmpty(itemId) && slot < 0) // Gets the first available non-tokenized item
                slot = i;
        }

        // Returns token to wallet if inventory cannot be changed
        if (slot < 0) {
            try {
                TokenWallet wallet = enjPlayer.getTokenWallet();
                MutableBalance balance = wallet.getBalance(id);
                balance.deposit(is.getAmount());
                return;
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }
        }

        Player player = enjPlayer.getBukkitPlayer();
        player.getWorld().dropItemNaturally(player.getLocation(), inventory.getItem(slot));
        inventory.clear(slot);
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
        String tokenId = TokenUtils.getTokenID(is);
        if (StringUtils.isEmpty(tokenId))
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
        String tokenId = TokenUtils.getTokenID(is);
        if (StringUtils.isEmpty(tokenId))
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
            String    id   = TokenUtils.getTokenID(is);
            if (view.getInventory(slot) != player.getInventory() && !StringUtils.isEmpty(id)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        CraftingInventory inventory = event.getInventory();
        for (int i = inventory.getSize() - 1; i >= 0; i--) {
            ItemStack is = inventory.getItem(i);
            String    id = TokenUtils.getTokenID(is);
            if (StringUtils.isEmpty(id))
                continue;

            event.setCancelled(true);
            break;
        }
    }

    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent event) {
        ItemStack held    = event.getPlayerItem();
        String    tokenId = TokenUtils.getTokenID(held);
        if (StringUtils.isEmpty(tokenId))
            return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof ItemFrame)
            interactItemFrame(event, (ItemFrame) entity);
    }

    private void interactItemFrame(PlayerInteractEntityEvent event, ItemFrame itemFrame) {
        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        if (itemFrame.getItem().getType() != Material.AIR)
            return;

        String tokenId = TokenUtils.getTokenID(held);
        if (StringUtils.isEmpty(tokenId))
            return;

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

        String tokenId = TokenUtils.getTokenID(bucket);
        if (StringUtils.isEmpty(tokenId))
            return;

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
