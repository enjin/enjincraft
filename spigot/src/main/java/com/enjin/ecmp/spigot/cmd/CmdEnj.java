package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.UnregisteredPlayerException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdEnj extends EnjCommand implements CommandExecutor, TabCompleter {

    private CmdBalance cmdBalance;
    private CmdHelp cmdHelp;
    private CmdLink cmdLink;
    private CmdSend cmdSend;
    private CmdTrade cmdTrade;
    private CmdUnlink cmdUnlink;
    private CmdWallet cmdWallet;

    public CmdEnj(SpigotBootstrap bootstrap) {
        super(bootstrap);
        this.aliases.add("ecmp");
        this.aliases.add("enj");
        this.addSubCommand(cmdBalance = new CmdBalance(bootstrap));
        this.addSubCommand(cmdHelp = new CmdHelp(bootstrap));
        this.addSubCommand(cmdLink = new CmdLink(bootstrap));
        this.addSubCommand(cmdSend = new CmdSend(bootstrap));
        this.addSubCommand(cmdTrade = new CmdTrade(bootstrap));
        this.addSubCommand(cmdUnlink = new CmdUnlink(bootstrap));
        this.addSubCommand(cmdWallet = new CmdWallet(bootstrap));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            process(new CommandContext(sender, new ArrayList<>(Arrays.asList(args)), label), true);
        } catch (UnregisteredPlayerException ex) {
            ex.printStackTrace();
        }

        return true;
    }

    @Override
    public void execute(CommandContext context) {
        if (cmdHelp.requirements.areMet(context, false))
            cmdHelp.execute(context);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        CommandContext context = new CommandContext(sender, new ArrayList<>(Arrays.asList(args)), label);
        process(context, false);
        return context.tabCompletionResult;
    }
}
