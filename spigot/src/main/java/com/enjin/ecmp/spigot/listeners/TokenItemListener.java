package com.enjin.ecmp.spigot.listeners;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.ECPlayer;
import com.enjin.ecmp.spigot.util.TokenUtils;
import com.enjin.ecmp.spigot.wallet.MutableBalance;
import com.enjin.ecmp.spigot.wallet.TokenWallet;
import com.enjin.java_commons.StringUtils;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TokenItemListener implements Listener {

    private SpigotBootstrap bootstrap;

    public TokenItemListener(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getKeepInventory()) {
            List<ItemStack> drops = event.getDrops();
            ECPlayer player = bootstrap.getPlayerManager().getPlayer(event.getEntity().getUniqueId());
            TokenWallet wallet = player.getTokenWallet();

            for (int i = drops.size() - 1; i >= 0; i--) {
                String id = TokenUtils.getTokenID(drops.get(i));
                if (id != null) {
                    ItemStack is = drops.remove(i);
                    MutableBalance balance = wallet.getBalance(id);
                    balance.deposit(is.getAmount());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        String id = TokenUtils.getTokenID(item.getItemStack());
        event.setCancelled(id != null);
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        CraftingInventory inventory = event.getInventory();
        for (int i = inventory.getSize() - 1; i >= 0; i--) {
            ItemStack is = inventory.getItem(i);
            String id = TokenUtils.getTokenID(is);
            if (!StringUtils.isEmpty(id)) {
                event.setCancelled(true);
                return;
            }
        }
    }

}
