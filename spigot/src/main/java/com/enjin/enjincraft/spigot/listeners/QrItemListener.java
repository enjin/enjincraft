package com.enjin.enjincraft.spigot.listeners;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.util.QrUtils;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.*;

import java.util.Map;

public class QrItemListener implements Listener {

    private final SpigotBootstrap bootstrap;

    private QrItemListener() {
        throw new IllegalStateException();
    }

    public QrItemListener(@NonNull SpigotBootstrap bootstrap) throws NullPointerException {
        this.bootstrap = bootstrap;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().forEach(is -> {
            if (QrUtils.hasQrTag(is))
                is.setAmount(0);
        });
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item      item = event.getItemDrop();
        ItemStack is   = item.getItemStack();
        if (QrUtils.hasQrTag(is))
            event.getItemDrop().remove();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        if (inventory instanceof PlayerInventory && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            /* Condition is associated with the standard player inventory view which allows the player to use
             * hot-key to move items within their own inventory. The top inventory is assumed to be the
             * 2x2 crafting menu.
             */
            Inventory top = event.getView().getTopInventory();
            if (top instanceof CraftingInventory && top.getSize() == 5)
                return;

            ItemStack is = event.getCurrentItem();
            if (QrUtils.hasQrTag(is))
                event.setCancelled(true);
        } else if (inventory != null && !(inventory instanceof PlayerInventory)) {
            ItemStack is = event.getCursor();
            if (QrUtils.hasQrTag(is))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryView view   = event.getView();
        Player        player = (Player) view.getPlayer();

        for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            Integer   slot = entry.getKey();
            ItemStack is   = entry.getValue();
            if (view.getInventory(slot) != player.getInventory() && QrUtils.hasQrTag(is)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        CraftingInventory inventory = event.getInventory();
        inventory.forEach(is -> {
            if (QrUtils.hasQrTag(is))
                event.setCancelled(true);
        });
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof ItemFrame)
            interactItemFrame(event, (ItemFrame) entity);
    }

    private void interactItemFrame(PlayerInteractEntityEvent event, ItemFrame itemFrame) {
        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        if (itemFrame.getItem().getType() == Material.AIR && QrUtils.hasQrTag(held))
            event.setCancelled(true);
    }

}
