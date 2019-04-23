package com.enjin.enjincoin.spigot_framework.listeners;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.inventory.WalletCheckoutManager;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.util.TokenUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * <p>A listener for handling events for which queries and updates to
 * an {@link Player} interactions take place.</p>
 *
 * @since 1.0
 */
public class PlayerInteractionListener implements Listener {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    /**
     * <p>Listener constructor.</p>
     *
     * @param main the Spigot plugin
     */
    public PlayerInteractionListener(BasePlugin main) {
        this.main = main;
    }

    /**
     * <p>Handle player interaction events.</p>
     *
     * @param event the event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // DEBUG ONLY
//        if (event.getAction() != null)
//            System.out.println("ACTION " + event.getAction());
//        if (event.getPlayer().getLocation() != null)
//            System.out.println("LOCATION " + event.getPlayer().getLocation());
//        if (event.getMaterial() != null)
//            System.out.println("WITH material " + event.getMaterial());
//        if (event.getItem() != null)
//            System.out.println("WITH itemStack " + ChatColor.stripColor(event.getItem().getItemMeta().getDisplayName()));
//        if (event.useInteractedBlock() != null)
//            System.out.println("USE INTERACTED BLOCK " + event.useInteractedBlock());
//        if (event.useItemInHand() != null)
//            System.out.println("USE ITEM IN HAND " + event.useItemInHand());

        if (event.getItem() != null && event.useInteractedBlock() != null && event.useItemInHand() != null) { // use selected item in hand
            String name = ChatColor.stripColor(event.getItem().getItemMeta().getDisplayName());
//            System.out.println("Attempting to use " + name);

            MinecraftPlayer mcplayer = this.main.getBootstrap().getPlayerManager().getPlayer(event.getPlayer().getUniqueId());
            try {
                WalletCheckoutManager checkout = mcplayer.getWallet().getCheckoutManager();

                String tokenId = TokenUtils.getTokenID(event.getItem());
                if (tokenId != null) { // item is tagged as a cryptoItem
                    // do thing with item based on tagged type: CONSUMABLE, REUSABLE, etc.
                    int amount = event.getItem().getAmount();

                    // get a copy of the item so we can replace it in the inventory after use.
                    // This is used for consumable items who's quantity is cannot be represented in a stack.
                    ItemStack clone = event.getItem().clone();
                }
            } catch (Exception e) {
            }
        }

    }
}
