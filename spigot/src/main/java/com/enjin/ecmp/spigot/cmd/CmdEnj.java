package com.enjin.ecmp.spigot.cmd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;

public class CmdEnj extends EnjCommand implements CommandExecutor {

    private CmdLink cmdLink = new CmdLink();

    public CmdEnj() {
        super();

        this.addSubCommand(cmdLink);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        process(new CommandContext(sender, new ArrayList<>(Arrays.asList(args)), label));
        return true;
    }

    @Override
    public void execute(CommandContext context) {
    }

}
