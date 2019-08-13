package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;

public class CmdEnj extends EnjCommand implements CommandExecutor {

    private SpigotBootstrap bootstrap;
    private CmdBalance cmdBalance;
    private CmdHelp cmdHelp;
    private CmdLink cmdLink;
    private CmdMenu cmdMenu;
    private CmdUnlink cmdUnlink;

    public CmdEnj(SpigotBootstrap bootstrap) {
        super();
        this.bootstrap = bootstrap;
        this.addSubCommand(cmdBalance = new CmdBalance(bootstrap));
        this.addSubCommand(cmdHelp = new CmdHelp(bootstrap));
        this.addSubCommand(cmdLink = new CmdLink(bootstrap));
        this.addSubCommand(cmdMenu = new CmdMenu(bootstrap));
        this.addSubCommand(cmdUnlink = new CmdUnlink(bootstrap));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        process(new CommandContext(sender, new ArrayList<>(Arrays.asList(args)), label));
        return true;
    }

    @Override
    public void execute(CommandContext context) {
        cmdHelp.execute(context);
    }

}
