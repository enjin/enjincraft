package com.enjin.ecmp.spigot_framework;

import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TokenDefinition {

    public static final String NBT_ID = "tokenID";

    private String id;
    private NBTItem nbtItemStack;

    public TokenDefinition(String id, JsonObject json) {
        this.id = id;
    }

    protected void setItemStack(JsonObject json) {
        if (!json.has("material"))
            throw new RuntimeException("Token definition is missing the material field.");

        String mat = json.get("material").getAsString();
        Material material = Material.getMaterial(mat);
        // If the material returned null try getting the material using legacy names
        if (material == null) material = Material.getMaterial(mat, true);
        // If the material returned null for both non-legacy and legacy names use an apple as material
        if (material == null) material = Material.APPLE;

        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();

        if (json.has("displayName"))
            meta.setDisplayName(ChatColor.DARK_PURPLE + json.get("displayName").getAsString());
        else
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Token #" + id);

        if (json.has("lore")) {
            List<String> lore = new ArrayList<>();

            JsonElement loreElem = json.get("lore");
            if (loreElem.isJsonArray()) {
                JsonArray loreArray = loreElem.getAsJsonArray();
                for (JsonElement line : loreArray) {
                    lore.add(ChatColor.DARK_GRAY + line.getAsString());
                }
            } else {
                lore.add(ChatColor.DARK_GRAY + loreElem.getAsString());
            }

            meta.setLore(lore);
        }

        itemStack.setItemMeta(meta);

        nbtItemStack = new NBTItem(itemStack);
        nbtItemStack.setString(NBT_ID, id);
    }

    public ItemStack getItemStackInstance() {
        return nbtItemStack.getItemStack().clone();
    }

}
