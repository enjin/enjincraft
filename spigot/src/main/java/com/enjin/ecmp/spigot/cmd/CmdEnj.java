package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.CommandProcess;
import com.enjin.ecmp.spigot.player.UnregisteredPlayerException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdEnj extends EnjCommand implements CommandExecutor, TabCompleter {

    protected CmdBalance cmdBalance;
    protected CmdHelp cmdHelp;
    protected CmdLink cmdLink;
    protected CmdSend cmdSend;
    protected CmdTrade cmdTrade;
    protected CmdUnlink cmdUnlink;
    protected CmdWallet cmdWallet;

    public CmdEnj(SpigotBootstrap bootstrap) {
        super(bootstrap);
        this.aliases.add("enj");
        this.addSubCommand(cmdBalance = new CmdBalance(bootstrap, this));
        this.addSubCommand(cmdHelp = new CmdHelp(bootstrap, this));
        this.addSubCommand(cmdLink = new CmdLink(bootstrap, this));
        this.addSubCommand(cmdSend = new CmdSend(bootstrap, this));
        this.addSubCommand(cmdTrade = new CmdTrade(bootstrap, this));
        this.addSubCommand(cmdUnlink = new CmdUnlink(bootstrap, this));
        this.addSubCommand(cmdWallet = new CmdWallet(bootstrap, this));
    }

    @Override
    public String getUsage(CommandContext context) {
        return super.getUsage(new CommandContext(context.sender, new ArrayList<>(), context.alias));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            process(new CommandContext(sender, new ArrayList<>(Arrays.asList(args)), label), CommandProcess.EXECUTE);
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
        process(context, CommandProcess.TAB);
        return context.tabCompletionResult;
    }
}
