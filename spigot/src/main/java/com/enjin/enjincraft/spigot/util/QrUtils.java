package com.enjin.enjincraft.spigot.util;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class QrUtils {

    public static final String QR_TAG = "enjinQr";

    private QrUtils() {
        throw new IllegalStateException();
    }

    public static boolean hasQrTag(ItemStack is) {
        if (is == null || is.getType() != Material.FILLED_MAP)
            return false;

        NBTItem nbtItem = new NBTItem(is);

        return nbtItem.hasKey(QR_TAG);
    }

}
