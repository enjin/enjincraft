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
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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
            List<ItemStack> drops = event.getDrops();
            Optional<EnjPlayer> optionalPlayer = bootstrap.getPlayerManager().getPlayer(event.getEntity());
            if (!optionalPlayer.isPresent())
                return;
            EnjPlayer player = optionalPlayer.get();
            TokenWallet wallet = player.getTokenWallet();

            for (int i = drops.size() - 1; i >= 0; i--) {
                String id = TokenUtils.getTokenID(drops.get(i));

                if (StringUtils.isEmpty(id))
                    continue;

                ItemStack is = drops.remove(i);
                MutableBalance balance = wallet.getBalance(id);
                balance.deposit(is.getAmount());
            }
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack is = event.getItemDrop().getItemStack();
        String    id = TokenUtils.getTokenID(is);

        Optional<EnjPlayer> optionalPlayer = bootstrap.getPlayerManager().getPlayer(event.getPlayer());

        if (!optionalPlayer.isPresent() || StringUtils.isEmpty(id))
            return;

        event.setCancelled(true);

        PlayerInventory inventory = event.getPlayer().getInventory();
        int size = inventory.getSize() - (inventory.getArmorContents().length + inventory.getExtraContents().length);
        int idx = -1;

        // Checks for available space
        for (int i = 0; i < size; i++) {
            ItemStack inventoryItem = inventory.getItem(i);
            String    inventoryId   = TokenUtils.getTokenID(inventoryItem);

            if (inventoryId == null) {
                return;
            } else if (StringUtils.isEmpty(inventoryId) && idx < 0) { // Gets the first available non-tokenized item
                idx = i;
                continue;
            }

            if (TokenUtils.canCombineStacks(is, inventoryItem))
                return;
        }

        Player player = event.getPlayer();

        // Returns token to wallet if inventory cannot be changed
        if (idx < 0) {
            try {
                TokenWallet wallet = optionalPlayer.get().getTokenWallet();
                MutableBalance balance = wallet.getBalance(id);
                balance.deposit(is.getAmount());
            } catch (Exception e) {
                bootstrap.log(e);
            }

            return;
        }

        player.getWorld().dropItemNaturally(player.getLocation(), inventory.getItem(idx));
        inventory.clear(idx);
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
             * hot-key move/equip items within their own inventory. The top inventory is the assumed to be the
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
        Player player = (Player) event.getView().getPlayer();
        if (AbstractMenu.hasAnyMenu(player))
            return;

        Inventory top = event.getView().getTopInventory();
        Inventory bottom = event.getView().getBottomInventory();

        int playerUpper;
        int playerLower;
        if (top instanceof PlayerInventory == bottom instanceof PlayerInventory)  {
            return;
        } else if (top instanceof PlayerInventory) {
            playerUpper = top.getSize();
            playerLower = 0;
        } else {
            playerUpper = bottom.getSize() + top.getSize();
            playerLower = top.getSize();
        }

        AtomicBoolean otherModified = new AtomicBoolean(false); // Assume false
        Map<Integer, ItemStack> newItems = event.getNewItems();

        // Checks if a token was placed outside the player's inventory
        newItems.forEach((rawSlot, is) -> {
            String tokenId = TokenUtils.getTokenID(is);

            if (!StringUtils.isEmpty(tokenId) && (rawSlot < playerLower || rawSlot >= playerUpper))
                otherModified.set(true);
        });

        event.setCancelled(otherModified.get());
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

}
