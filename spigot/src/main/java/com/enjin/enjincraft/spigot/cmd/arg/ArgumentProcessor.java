package com.enjin.enjincraft.spigot.cmd.arg;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface ArgumentProcessor<T> {

    List<String> tab(CommandSender sender, String arg);

    T parse(CommandSender sender, String arg);

}
