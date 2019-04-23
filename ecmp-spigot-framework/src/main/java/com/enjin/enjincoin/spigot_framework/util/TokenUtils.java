package com.enjin.enjincoin.spigot_framework.util;

import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import org.bukkit.inventory.ItemStack;

public class TokenUtils {

    public static String getTokenID(ItemStack is) {
        if (is != null) {
            NBTItem nbt = new NBTItem(is);

            if (nbt.hasKey("tokenID")) {
                return nbt.getString("tokenID");
            }
        }

        return null;
    }

}
