package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.EnjSpigot;
import com.enjin.ecmp.spigot.cmd.arg.PlayerArgumentProcessor;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.player.UnregisteredPlayerException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

public class CommandContext {

    protected CommandSender sender;
    protected SenderType senderType;
    protected Player player;
    protected EnjPlayer enjPlayer;
    protected List<String> args;
    protected String alias;
    protected Stack<EnjCommand> commandStack;
    protected List<String> tabCompletionResult;

    public CommandContext(CommandSender sender, List<String> args, String alias) {
        this.sender = sender;
        this.senderType = SenderType.type(sender);
        this.args = args;
        this.alias = alias;
        this.commandStack = new Stack<>();
        this.tabCompletionResult = new ArrayList<>();

        if (sender instanceof Player) {
            player = (Player) sender;
            enjPlayer = EnjSpigot.bootstrap().get()
                    .getPlayerManager()
                    .getPlayer(player)
                    .orElse(null);

            if (enjPlayer == null) {
                throw new UnregisteredPlayerException(player);
            }
        }
    }

    public Optional<Player> argToPlayer(int index) {
        if (args.size() == 0 || index >= args.size()) return Optional.empty();
        return PlayerArgumentProcessor.INSTANCE.parse(sender, args.get(index));
    }

    public static List<EnjCommand> createCommandStackAsList(EnjCommand top) {
        List<EnjCommand> list = new ArrayList<>();

        list.add(top);
        Optional<EnjCommand> parent = top.parent;
        while (parent.isPresent()) {
            list.add(0, parent.get());
            parent = parent.get().parent;
        }

        return list;
    }

}
