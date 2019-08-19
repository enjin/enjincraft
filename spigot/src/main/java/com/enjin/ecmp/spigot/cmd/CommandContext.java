package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.EnjSpigot;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.player.UnregisteredPlayerException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class CommandContext {

    protected CommandSender sender;
    protected SenderType senderType;
    protected Player player;
    protected EnjPlayer enjPlayer;
    protected List<String> args;
    protected String alias;
    protected Stack<EnjCommand> commandStack;

    public CommandContext(CommandSender sender, List<String> args, String alias) {
        this.sender = sender;
        this.senderType = SenderType.type(sender);
        this.args = args;
        this.alias = alias;
        this.commandStack = new Stack<>();

        if (sender instanceof Player) {
            player = (Player) sender;
            enjPlayer = EnjSpigot.bootstrap()
                    .getPlayerManager()
                    .getPlayer(player);

            if (enjPlayer == null) {
                throw new UnregisteredPlayerException(player);
            }
        }
    }

}
