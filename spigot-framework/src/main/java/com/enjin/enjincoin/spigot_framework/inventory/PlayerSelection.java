package com.enjin.enjincoin.spigot_framework.inventory;

import com.enjin.enjincoin.spigot_framework.ui.SkullMenuItem;
import com.enjin.minecraft_commons.spigot.ui.Menu;
import com.enjin.minecraft_commons.spigot.ui.MenuItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

import java.util.UUID;

public class PlayerSelection extends Menu {

    private static final String TITLE = ChatColor.DARK_PURPLE + "Player Selection";
    private static final int HEAD_START_INDEX = 10;
    private static final int MAX_HEADS = 9 * 4;
    private static final int HEAD_END_INDEX = HEAD_START_INDEX + MAX_HEADS;

    private UUID playerUuid;

    public PlayerSelection(UUID playerUuid) {
        super(TITLE, 6);
        this.playerUuid = playerUuid;

        setPlayerHeads();
        setCancelButton();

        updateInventory();
    }

    private void setPlayerHeads() {
        int index = HEAD_START_INDEX;
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (index >= HEAD_END_INDEX) {
                break;
            }

            if (other.getUniqueId().equals(playerUuid)) {
                continue;
            }

            addMenuItem(createPlayerHeadItem(other), index++);
        }
    }

    private void setCancelButton() {
        addMenuItem(createCancelItem(), 4, 5);
    }

    private MenuItem createPlayerHeadItem(Player player) {
        String title = ChatColor.LIGHT_PURPLE + player.getName();

        return new SkullMenuItem(title, player) {
            @Override
            public void onClick(Player player) {
                // TODO
            }
        };
    }

    private MenuItem createCancelItem() {
        String title = ChatColor.RED + "Cancel";
        MaterialData data = new MaterialData(Material.BARRIER);

        return new MenuItem(title, data) {
            @Override
            public void onClick(Player player) {
                closeMenu(player);
            }
        };
    }

}
