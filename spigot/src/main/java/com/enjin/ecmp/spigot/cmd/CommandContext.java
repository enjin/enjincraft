package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.EcmpSpigot;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandContext {

    protected CommandSender sender;
    protected Player player;
    protected EnjPlayer enjPlayer;
    protected List<String> args;
    protected String alias;
    protected List<EnjCommand> commandStack;

    public CommandContext(CommandSender sender, List<String> args, String alias) {
        this.sender = sender;
        this.args = args;
        this.alias = alias;
        this.commandStack = new ArrayList<>();

        if (sender instanceof Player) {
            player = (Player) sender;
            enjPlayer = EcmpSpigot.bootstrap()
                    .getPlayerManager()
                    .getPlayer(player);
        }
    }

}
