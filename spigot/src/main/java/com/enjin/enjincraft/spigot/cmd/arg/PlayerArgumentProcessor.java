package com.enjin.enjincraft.spigot.cmd.arg;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class PlayerArgumentProcessor extends AbstractArgumentProcessor<Player> {

    public static final PlayerArgumentProcessor INSTANCE = new PlayerArgumentProcessor();

    @Override
    public List<String> tab(CommandSender sender, String arg) {
        String lowerCaseArg = arg.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(p -> p.getName().toLowerCase())
                .filter(name -> name.startsWith(lowerCaseArg))
                .collect(Collectors.toList());
    }

    @Override
    public Player parse(CommandSender sender, String arg) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(arg) || player.getUniqueId().toString().equalsIgnoreCase(arg))
                return player;
        }

        return null;
    }

}
