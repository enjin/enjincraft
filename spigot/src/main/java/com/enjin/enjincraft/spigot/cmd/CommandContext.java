package com.enjin.enjincraft.spigot.cmd;

import com.enjin.enjincraft.spigot.Bootstrap;
import com.enjin.enjincraft.spigot.cmd.arg.PlayerArgumentProcessor;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.player.UnregisteredPlayerException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CommandContext {

    protected CommandSender sender;
    protected SenderType senderType;
    @Nullable
    protected Player player;
    @Nullable
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
            this.player = (Player) sender;
            this.enjPlayer = bootstrap.getPlayerManager()
                    .getPlayer(this.player);
            if (this.enjPlayer == null)
                throw new UnregisteredPlayerException(player);
        }
    }

    public Player argToPlayer(int index) {
        if (args.isEmpty() || index >= args.size())
            return null;

        return PlayerArgumentProcessor.INSTANCE.parse(sender, args.get(index));
    }

    public Integer argToInt(int index) {
        return index < args.size()
                ? Integer.parseInt(args.get(index))
                : null;
    }

    public static List<EnjCommand> createCommandStackAsList(EnjCommand top) {
        List<EnjCommand> list = new ArrayList<>();

        list.add(top);
        EnjCommand parent = top.parent;
        while (parent != null) {
            list.add(0, parent);
            parent = parent.parent;
        }

        return list;
    }

}
