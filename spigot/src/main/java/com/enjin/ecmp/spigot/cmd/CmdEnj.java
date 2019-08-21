package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.CommandProcess;
import com.enjin.ecmp.spigot.enums.MessageAction;
import com.enjin.ecmp.spigot.i18n.Translation;
import com.enjin.ecmp.spigot.player.UnregisteredPlayerException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdEnj extends EnjCommand implements CommandExecutor, TabCompleter {

    public CmdEnj(SpigotBootstrap bootstrap) {
        super(bootstrap);
        this.aliases.add("enj");
        this.addSubCommand(new CmdBalance(bootstrap, this));
        this.addSubCommand(new CmdHelp(bootstrap, this));
        this.addSubCommand(new CmdLink(bootstrap, this));
        this.addSubCommand(new CmdSend(bootstrap, this));
        this.addSubCommand(new CmdConfSet(bootstrap, this));
        this.addSubCommand(new CmdTrade(bootstrap, this));
        this.addSubCommand(new CmdUnlink(bootstrap, this));
        this.addSubCommand(new CmdWallet(bootstrap, this));
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
        // TODO: show plugin information
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        CommandContext context = new CommandContext(sender, new ArrayList<>(Arrays.asList(args)), label);
        process(context, CommandProcess.TAB);
        return context.tabCompletionResult;
    }

    @Override
    public Translation getUsageTranslation() {
        return Translation.COMMAND_ROOT_DESCRIPTION;
    }

}
