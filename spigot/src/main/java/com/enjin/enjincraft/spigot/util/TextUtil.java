package com.enjin.enjincraft.spigot.util;

import org.bukkit.ChatColor;

import java.util.List;

public class TextUtil {

    public static String concat(List<String> list, String glue) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            if (i > 0)
                builder.append(glue);
            builder.append(list.get(i));
        }

        return builder.toString();
    }

    public static String colorize(char prefix, String text) {
        return ChatColor.translateAlternateColorCodes(prefix, text);
    }

    public static String colorize(String text) {
        return colorize('&', text);
    }

}
