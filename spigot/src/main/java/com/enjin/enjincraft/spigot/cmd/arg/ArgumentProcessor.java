package com.enjin.enjincraft.spigot.cmd.arg;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;

public interface ArgumentProcessor<T> {

    List<String> tab(CommandSender sender, String arg);

    <A extends Object> Optional<T> parse(CommandSender sender, String arg);

}
