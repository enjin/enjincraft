package com.enjin.enjincraft.spigot.configuration;

import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TokenDefinition {

    public static final String NBT_ID = "tokenID";

    private String id;
    private String displayName;
    private NBTItem nbtItemStack;

    public TokenDefinition(String id, JsonObject json) {
        this.id = id;
        this.displayName = json.has(TokenConfKeys.ITEM_DISPLAY_NAME)
                ? json.get(TokenConfKeys.ITEM_DISPLAY_NAME).getAsString() : id;
        setItemStack(json);
    }

    protected void setItemStack(JsonObject json) {
        if (!json.has(TokenConfKeys.ITEM_MATERIAL))
            throw new TokenConfigurationException(id, TokenConfKeys.ITEM_MATERIAL);

        String mat = json.get(TokenConfKeys.ITEM_MATERIAL).getAsString();
        Material material = Material.getMaterial(mat);
        // If the material returned null try getting the material using legacy names
        if (material == null) material = Material.getMaterial(mat, true);
        // If the material returned null for both non-legacy and legacy names use an apple as material
        if (material == null) material = Material.APPLE;

        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();

        if (json.has(TokenConfKeys.ITEM_DISPLAY_NAME))
            meta.setDisplayName(ChatColor.DARK_PURPLE + json.get(TokenConfKeys.ITEM_DISPLAY_NAME).getAsString());
        else
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Token #" + id);

        if (json.has(TokenConfKeys.ITEM_LORE)) {
            List<String> lore = new ArrayList<>();

            JsonElement loreElem = json.get(TokenConfKeys.ITEM_LORE);
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

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        itemStack.setItemMeta(meta);

        nbtItemStack = new NBTItem(itemStack);
        nbtItemStack.setString(NBT_ID, id);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ItemStack getItemStackInstance() {
        return nbtItemStack.getItemStack().clone();
    }

}
