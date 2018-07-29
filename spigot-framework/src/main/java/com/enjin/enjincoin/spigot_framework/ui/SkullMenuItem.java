package com.enjin.enjincoin.spigot_framework.ui;

import com.enjin.minecraft_commons.spigot.ui.MenuItem;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * <p>A MenuItem that represents a player skull.</p>
 */
public abstract class SkullMenuItem extends MenuItem {

    private ItemStack skullItemStack;

    public SkullMenuItem(String text, OfflinePlayer player) {
        super(text);
        createSkullItemStack(text, player);
    }

    private void createSkullItemStack(String text, OfflinePlayer player) {
        this.skullItemStack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) this.skullItemStack.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(text);
        this.skullItemStack.setItemMeta(meta);
    }

    @Override
    public void setQuantity(int quantity) {
        super.setQuantity(quantity);
        this.skullItemStack.setAmount(quantity);
    }

    @Override
    public ItemStack getItemStack() {
        return this.skullItemStack.clone();
    }

    @Override
    public ItemStack getSingleItemStack() {
        ItemStack clone = this.skullItemStack.clone();
        clone.setAmount(1);
        return clone;
    }
}
