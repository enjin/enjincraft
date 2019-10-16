package com.enjin.enjincraft.spigot.util;

import com.enjin.enjincraft.spigot.configuration.TokenDefinition;
import com.enjin.minecraft_commons.spigot.nbt.NBTItem;
import org.bukkit.inventory.ItemStack;

public class TokenUtils {

    public static String getTokenID(ItemStack is) {
        if (is != null) {
            NBTItem nbt = new NBTItem(is);

            if (nbt.hasKey(TokenDefinition.NBT_ID))
                return nbt.getString(TokenDefinition.NBT_ID);
        }

        return null;
    }

}
