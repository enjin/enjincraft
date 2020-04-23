package com.enjin.enjincraft.spigot.listeners;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.util.StringUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.wallet.MutableBalance;
import com.enjin.enjincraft.spigot.wallet.TokenWallet;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        Item item = event.getItemDrop();
        String id = TokenUtils.getTokenID(item.getItemStack());
        event.setCancelled(id != null);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack clk = event.getCurrentItem();
        ItemStack held = event.getCursor();

        if (held != null && held.getType() != Material.AIR)
            InventoryClickHolding(event, held);
        else if (clk != null && clk.getType() != Material.AIR)
            InventoryClickClicked(event, clk);
    }

    private void InventoryClickClicked(InventoryClickEvent event, ItemStack is) {
        String tokenId = TokenUtils.getTokenID(is);

        if (StringUtils.isEmpty(tokenId))
            return;

        Inventory clkInv = event.getClickedInventory();

        /* TODO: Ought to check if the token belongs to the player and if not
         *       is the player linked?
         */

        if (!(clkInv instanceof PlayerInventory) || event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY)
            return;

        event.setCancelled(true);
    }

    private void InventoryClickHolding(InventoryClickEvent event, ItemStack is) {
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
            if (is == null || is.getType() == Material.AIR)
                return;

            String tokenId = TokenUtils.getTokenID(is);

            // TODO: Ought to check if the token belongs to the player

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
            String id = TokenUtils.getTokenID(is);

            if (StringUtils.isEmpty(id))
                continue;

            event.setCancelled(true);
            break;
        }
    }

    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent event) {
        ItemStack held = event.getPlayerItem();

        if (held.getType() == Material.AIR || event.getArmorStandItem().getType() != Material.AIR)
            return;

        String tokenId = TokenUtils.getTokenID(held);

        if (StringUtils.isEmpty(tokenId))
            return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();

        if (entity instanceof ItemFrame)
            InteractItemFrame(event, (ItemFrame) entity);
    }

    private void InteractItemFrame(PlayerInteractEntityEvent event, ItemFrame itemFrame) {
        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();

        if (held.getType() == Material.AIR || itemFrame.getItem().getType() != Material.AIR)
            return;

        String tokenId = TokenUtils.getTokenID(held);

        if (StringUtils.isEmpty(tokenId))
            return;

        event.setCancelled(true);
    }

}
