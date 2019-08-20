package com.enjin.ecmp.spigot.cmd.arg;

import com.enjin.ecmp.spigot.cmd.CommandContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PlayerArgument extends AbstractArgument<Player> {

    public static final PlayerArgument REQUIRED = new PlayerArgument();
    public static final PlayerArgument OPTIONAL = new PlayerArgument(false);

    public PlayerArgument(boolean required) {
        super(required);
    }

    public PlayerArgument() {
        super();
    }

    @Override
    public List<String> tab() {
        return Bukkit.getOnlinePlayers().stream()
                .map(p -> p.getName())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Player> parse(CommandContext context, List<String> args) {
        Optional<Player> result = Optional.empty();
        String name = args.get(0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                result = Optional.of(player);
                break;
            }
        }

        return result;
    }

}
