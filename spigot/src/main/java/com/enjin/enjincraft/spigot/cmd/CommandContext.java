package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.Bootstrap;
import com.enjin.enjincraft.spigot.cmd.arg.PlayerArgumentProcessor;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.player.UnregisteredPlayerException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandContext {

    protected CommandSender sender;
    protected SenderType senderType;
    protected Player player;
    protected EnjPlayer enjPlayer;
    protected List<String> args;
    protected String alias;
    protected Deque<EnjCommand> commandStack;
    protected List<String> tabCompletionResult;

    public CommandContext(Bootstrap bootstrap, CommandSender sender, List<String> args, String alias) {
        this.sender = sender;
        this.senderType = SenderType.type(sender);
        this.args = args;
        this.alias = alias;
        this.commandStack = new ArrayDeque<>();
        this.tabCompletionResult = new ArrayList<>();

        if (sender instanceof Player) {
            player = (Player) sender;
            enjPlayer = bootstrap.getPlayerManager()
                    .getPlayer(player)
                    .orElse(null);

            if (enjPlayer == null)
                throw new UnregisteredPlayerException(player);
        }
    }

    public Optional<Player> argToPlayer(int index) {
        if (args.isEmpty() || index >= args.size())
            return Optional.empty();
        return PlayerArgumentProcessor.INSTANCE.parse(sender, args.get(index));
    }

    public Optional<Integer> argToInt(int index) {
        Optional<Integer> result = Optional.empty();
        if (index < args.size())
            result = Optional.of(Integer.parseInt(args.get(index)));
        return result;
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
